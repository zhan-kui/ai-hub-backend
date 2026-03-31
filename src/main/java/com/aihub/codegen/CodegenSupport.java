package com.aihub.codegen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class CodegenSupport {

    private CodegenSupport() {
    }

    public record GeneratorConfig(
            String jdbcUrl,
            String username,
            String password,
            String basePackage,
            Path javaOutputDir,
            Path resourceOutputDir,
            List<String> includeTables,
            List<String> tablePrefixes,
            boolean overwrite,
            String author,
            String apiPrefix
    ) {
        public GeneratorConfig {
            Objects.requireNonNull(jdbcUrl, "jdbcUrl is required");
            Objects.requireNonNull(username, "username is required");
            Objects.requireNonNull(password, "password is required");
            Objects.requireNonNull(basePackage, "basePackage is required");
            Objects.requireNonNull(javaOutputDir, "javaOutputDir is required");
            Objects.requireNonNull(resourceOutputDir, "resourceOutputDir is required");
            includeTables = includeTables == null ? List.of() : List.copyOf(includeTables);
            tablePrefixes = tablePrefixes == null ? List.of() : List.copyOf(tablePrefixes);
            author = author == null ? "unknown" : author;
            apiPrefix = apiPrefix == null ? "/gen" : apiPrefix;
        }
    }

    public record TableMeta(
            String tableName,
            String className,
            String objectName,
            String tableComment,
            List<ColumnMeta> columns,
            ColumnMeta primaryKey
    ) {
        public String mapperName() {
            return className + "Mapper";
        }

        public String repositoryName() {
            return className + "Repository";
        }

        public String serviceName() {
            return className + "AutoService";
        }

        public String controllerName() {
            return className + "AutoController";
        }

        public String createDtoName() {
            return className + "CreateRequest";
        }

        public String updateDtoName() {
            return className + "UpdateRequest";
        }

        public String voName() {
            return className + "VO";
        }

        public String endpoint() {
            return "/" + toKebabCase(objectName);
        }

        public List<ColumnMeta> insertableColumns() {
            return columns.stream()
                    .filter(column -> !column.primaryKey())
                    .filter(column -> !isManagedColumn(column.columnName()))
                    .toList();
        }

        public List<ColumnMeta> updatableColumns() {
            return insertableColumns().stream()
                    .filter(column -> !"created_at".equalsIgnoreCase(column.columnName()))
                    .toList();
        }

        private boolean isManagedColumn(String columnName) {
            String lower = columnName.toLowerCase(Locale.ROOT);
            return "created_at".equals(lower)
                    || "updated_at".equals(lower)
                    || "deleted".equals(lower);
        }
    }

    public record ColumnMeta(
            String columnName,
            String fieldName,
            String javaType,
            int jdbcType,
            String typeName,
            String comment,
            boolean nullable,
            boolean primaryKey,
            boolean autoIncrement,
            int size,
            int scale
    ) {
        public boolean isTemporal() {
            return LocalDateTime.class.getSimpleName().equals(javaType)
                    || LocalDate.class.getSimpleName().equals(javaType)
                    || LocalTime.class.getSimpleName().equals(javaType);
        }

        public boolean isBigDecimal() {
            return "BigDecimal".equals(javaType);
        }

        public boolean isBoolean() {
            return "Boolean".equals(javaType);
        }

        public String jdbcTypeName() {
            return switch (jdbcType) {
                case Types.BIGINT -> "BIGINT";
                case Types.INTEGER -> "INTEGER";
                case Types.SMALLINT -> "SMALLINT";
                case Types.TINYINT -> "TINYINT";
                case Types.BIT, Types.BOOLEAN -> "BOOLEAN";
                case Types.DECIMAL, Types.NUMERIC -> "DECIMAL";
                case Types.FLOAT -> "FLOAT";
                case Types.DOUBLE -> "DOUBLE";
                case Types.REAL -> "REAL";
                case Types.DATE -> "DATE";
                case Types.TIME -> "TIME";
                case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "TIMESTAMP";
                case Types.CHAR -> "CHAR";
                case Types.VARCHAR -> "VARCHAR";
                case Types.LONGVARCHAR -> "LONGVARCHAR";
                case Types.CLOB -> "CLOB";
                case Types.BINARY -> "BINARY";
                case Types.VARBINARY -> "VARBINARY";
                case Types.LONGVARBINARY -> "LONGVARBINARY";
                case Types.BLOB -> "BLOB";
                default -> "OTHER";
            };
        }
    }

    public static List<TableMeta> loadTableMetas(GeneratorConfig config) {
        try (Connection connection = DriverManager.getConnection(
                config.jdbcUrl(), config.username(), config.password())) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = Optional.ofNullable(connection.getCatalog()).orElse(null);
            List<String> targetTables = resolveTables(metaData, catalog, config.includeTables());
            List<TableMeta> tableMetas = new ArrayList<>();
            for (String tableName : targetTables) {
                TableMeta tableMeta = loadTable(metaData, catalog, tableName, config.tablePrefixes());
                if (tableMeta.columns().isEmpty()) {
                    System.out.println("[WARN] skip table with no columns: " + tableName);
                    continue;
                }
                tableMetas.add(tableMeta);
            }
            tableMetas.sort(Comparator.comparing(TableMeta::tableName));
            return tableMetas;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load table metadata", exception);
        }
    }

    private static List<String> resolveTables(DatabaseMetaData metaData,
                                              String catalog,
                                              List<String> includeTables) throws SQLException {
        if (includeTables != null && !includeTables.isEmpty()) {
            return includeTables.stream().distinct().sorted().toList();
        }

        List<String> tables = new ArrayList<>();
        try (ResultSet tableRs = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (tableRs.next()) {
                String tableName = tableRs.getString("TABLE_NAME");
                if (tableName != null) {
                    tables.add(tableName);
                }
            }
        }
        tables.sort(String::compareToIgnoreCase);
        return tables;
    }

    private static TableMeta loadTable(DatabaseMetaData metaData,
                                       String catalog,
                                       String tableName,
                                       List<String> tablePrefixes) throws SQLException {
        String tableComment = "";
        try (ResultSet tableRs = metaData.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            if (tableRs.next()) {
                tableComment = Optional.ofNullable(tableRs.getString("REMARKS")).orElse("");
            }
        }

        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet pkRs = metaData.getPrimaryKeys(catalog, null, tableName)) {
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
        }

        Map<String, ColumnMeta> columns = new LinkedHashMap<>();
        try (ResultSet columnRs = metaData.getColumns(catalog, null, tableName, "%")) {
            while (columnRs.next()) {
                String columnName = columnRs.getString("COLUMN_NAME");
                int jdbcType = columnRs.getInt("DATA_TYPE");
                String typeName = columnRs.getString("TYPE_NAME");
                int size = columnRs.getInt("COLUMN_SIZE");
                int scale = columnRs.getInt("DECIMAL_DIGITS");
                boolean nullable = DatabaseMetaData.columnNullable == columnRs.getInt("NULLABLE");
                String comment = Optional.ofNullable(columnRs.getString("REMARKS")).orElse("");
                boolean primary = primaryKeys.contains(columnName);
                boolean autoIncrement = "YES".equalsIgnoreCase(columnRs.getString("IS_AUTOINCREMENT"));
                String fieldName = toCamelCase(columnName, false);
                String javaType = toJavaType(jdbcType, typeName, size, scale);
                columns.put(columnName, new ColumnMeta(
                        columnName,
                        fieldName,
                        javaType,
                        jdbcType,
                        typeName,
                        comment,
                        nullable,
                        primary,
                        autoIncrement,
                        size,
                        scale
                ));
            }
        }

        ColumnMeta primaryKey = columns.values().stream()
                .filter(ColumnMeta::primaryKey)
                .findFirst()
                .orElseGet(() -> columns.values().stream()
                        .filter(column -> "id".equalsIgnoreCase(column.columnName()))
                        .findFirst()
                        .orElse(null));

        String objectName = stripTablePrefix(tableName, tablePrefixes);
        String className = toCamelCase(objectName, true);

        return new TableMeta(
                tableName,
                className,
                objectName,
                tableComment,
                List.copyOf(columns.values()),
                primaryKey
        );
    }

    public static String toJavaType(int jdbcType, String typeName, int size, int scale) {
        return switch (jdbcType) {
            case Types.BIGINT -> "Long";
            case Types.INTEGER -> "Integer";
            case Types.SMALLINT -> "Integer";
            case Types.TINYINT -> size == 1 ? "Boolean" : "Integer";
            case Types.BIT, Types.BOOLEAN -> "Boolean";
            case Types.DECIMAL, Types.NUMERIC -> scale > 0 ? "BigDecimal" : "Long";
            case Types.FLOAT -> "Float";
            case Types.DOUBLE, Types.REAL -> "Double";
            case Types.DATE -> "LocalDate";
            case Types.TIME -> "LocalTime";
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "LocalDateTime";
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.CLOB -> "String";
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> "byte[]";
            default -> {
                String normalized = Optional.ofNullable(typeName).orElse("").toLowerCase(Locale.ROOT);
                if (normalized.contains("json") || normalized.contains("text")) {
                    yield "String";
                }
                yield "String";
            }
        };
    }

    public static String stripTablePrefix(String tableName, List<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return tableName;
        }
        for (String prefix : prefixes) {
            if (tableName.startsWith(prefix)) {
                return tableName.substring(prefix.length());
            }
        }
        return tableName;
    }

    public static String toCamelCase(String value, boolean capitalizeFirst) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String[] parts = value.split("[_\\-\\s]+");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isEmpty()) {
                continue;
            }

            String lower = part.toLowerCase(Locale.ROOT);
            if (index == 0 && !capitalizeFirst) {
                builder.append(lower);
            } else {
                builder.append(Character.toUpperCase(lower.charAt(0)));
                if (lower.length() > 1) {
                    builder.append(lower.substring(1));
                }
            }
        }
        return builder.toString();
    }

    public static String toKebabCase(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String snake = value
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replace('-', '_')
                .toLowerCase(Locale.ROOT);
        return snake.replace('_', '-');
    }

    public static void writeJavaFile(GeneratorConfig config,
                                     String packageName,
                                     String className,
                                     String content) {
        Path packageDir = config.javaOutputDir()
                .resolve(packageName.replace('.', '/'));
        Path targetFile = packageDir.resolve(className + ".java");
        writeFile(targetFile, content, config.overwrite());
    }

    public static void writeResourceFile(GeneratorConfig config,
                                         String relativePath,
                                         String content) {
        Path targetFile = config.resourceOutputDir().resolve(relativePath);
        writeFile(targetFile, content, config.overwrite());
    }

    public static void writeFile(Path targetFile, String content, boolean overwrite) {
        try {
            Files.createDirectories(targetFile.getParent());
            if (Files.exists(targetFile) && !overwrite) {
                System.out.println("[SKIP] " + targetFile + " already exists");
                return;
            }
            Files.writeString(targetFile, content, StandardCharsets.UTF_8);
            System.out.println("[WRITE] " + targetFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write file: " + targetFile, exception);
        }
    }

    public static Set<String> collectCommonImports(TableMeta tableMeta) {
        Set<String> imports = new LinkedHashSet<>();
        for (ColumnMeta column : tableMeta.columns()) {
            if (column.isTemporal()) {
                if ("LocalDateTime".equals(column.javaType())) {
                    imports.add("java.time.LocalDateTime");
                } else if ("LocalDate".equals(column.javaType())) {
                    imports.add("java.time.LocalDate");
                } else if ("LocalTime".equals(column.javaType())) {
                    imports.add("java.time.LocalTime");
                }
            }
            if (column.isBigDecimal()) {
                imports.add("java.math.BigDecimal");
            }
        }
        return imports;
    }
}
