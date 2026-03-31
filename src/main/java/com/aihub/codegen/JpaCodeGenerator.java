package com.aihub.codegen;

import com.aihub.codegen.CodegenSupport.ColumnMeta;
import com.aihub.codegen.CodegenSupport.GeneratorConfig;
import com.aihub.codegen.CodegenSupport.TableMeta;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JpaCodeGenerator {

    public static void main(String[] args) {
        GeneratorConfig config = defaultConfig();
        generate(config);
    }

    public static void generate(GeneratorConfig config) {
        List<TableMeta> tables = CodegenSupport.loadTableMetas(config);
        if (tables.isEmpty()) {
            System.out.println("No tables found, skip generating.");
            return;
        }

        for (TableMeta table : tables) {
            generateEntity(config, table);
            generateRepository(config, table);
            generateDto(config, table);
            generateService(config, table);
            generateController(config, table);
        }

        System.out.println("JPA scaffold generation finished. tables=" + tables.size());
    }

    private static GeneratorConfig defaultConfig() {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        return new GeneratorConfig(
                "jdbc:mysql://127.0.0.1:3306/aihub?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true",
                "root",
                "123456",
                "com.aihub",
                projectRoot.resolve("src/main/java"),
                projectRoot.resolve("src/main/resources"),
                List.of(),
                List.of("sys_"),
                false,
                "codex",
                "/gen/jpa"
        );
    }

    private static void generateEntity(GeneratorConfig config, TableMeta table) {
        String packageName = config.basePackage() + ".entity";
        Set<String> imports = new LinkedHashSet<>();
        imports.add("jakarta.persistence.Column");
        imports.add("jakarta.persistence.Entity");
        imports.add("jakarta.persistence.GeneratedValue");
        imports.add("jakarta.persistence.GenerationType");
        imports.add("jakarta.persistence.Id");
        imports.add("jakarta.persistence.Table");
        imports.add("lombok.Data");
        imports.addAll(CodegenSupport.collectCommonImports(table));

        StringBuilder body = new StringBuilder();
        body.append("package ").append(packageName).append(";\n\n");
        imports.forEach(importLine -> body.append("import ").append(importLine).append(";\n"));
        body.append("\n");

        appendClassComment(body, table.tableComment(), table.className(), config.author());
        body.append("@Data\n");
        body.append("@Entity\n");
        body.append("@Table(name = \"").append(table.tableName()).append("\")\n");
        body.append("public class ").append(table.className()).append(" {\n\n");

        for (ColumnMeta column : table.columns()) {
            appendFieldComment(body, column.comment());
            if (column.primaryKey()) {
                body.append("    @Id\n");
                if (column.autoIncrement()) {
                    body.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
                }
            }

            body.append("    @Column(name = \"").append(column.columnName()).append("\"");
            if (!column.nullable()) {
                body.append(", nullable = false");
            }
            if ("String".equals(column.javaType()) && column.size() > 0 && column.size() <= 65535) {
                body.append(", length = ").append(column.size());
            }
            body.append(")\n");

            body.append("    private ")
                    .append(column.javaType())
                    .append(" ")
                    .append(column.fieldName())
                    .append(";\n\n");
        }

        body.append("}\n");

        CodegenSupport.writeJavaFile(config, packageName, table.className(), body.toString());
    }

    private static void generateRepository(GeneratorConfig config, TableMeta table) {
        String packageName = config.basePackage() + ".repository.generated";
        String pkType = primaryKeyType(table);

        String content = """
                package %s;

                import %s.%s;
                import org.springframework.data.jpa.repository.JpaRepository;

                public interface %s extends JpaRepository<%s, %s> {
                }
                """.formatted(
                packageName,
                config.basePackage() + ".entity",
                table.className(),
                repositoryName(table),
                table.className(),
                pkType
        );

        CodegenSupport.writeJavaFile(config, packageName, repositoryName(table), content);
    }

    private static void generateDto(GeneratorConfig config, TableMeta table) {
        String dtoPackage = config.basePackage() + ".dto.generated.jpa." + table.objectName().toLowerCase(Locale.ROOT);

        CodegenSupport.writeJavaFile(config, dtoPackage, table.createDtoName(),
                buildRequestDto(dtoPackage, table.createDtoName(), table.insertableColumns(), true, table, config.author()));

        CodegenSupport.writeJavaFile(config, dtoPackage, table.updateDtoName(),
                buildRequestDto(dtoPackage, table.updateDtoName(), table.updatableColumns(), false, table, config.author()));

        CodegenSupport.writeJavaFile(config, dtoPackage, table.voName(),
                buildVoDto(dtoPackage, table, config.author()));
    }

    private static void generateService(GeneratorConfig config, TableMeta table) {
        String packageName = config.basePackage() + ".service.generated";
        String dtoPackage = config.basePackage() + ".dto.generated.jpa." + table.objectName().toLowerCase(Locale.ROOT);
        String repositoryPackage = config.basePackage() + ".repository.generated." + repositoryName(table);

        String pkType = primaryKeyType(table);

        StringBuilder body = new StringBuilder();
        body.append("package ").append(packageName).append(";\n\n");
        body.append("import ").append(config.basePackage()).append(".common.exception.BizException;\n");
        body.append("import ").append(config.basePackage()).append(".entity.").append(table.className()).append(";\n");
        body.append("import ").append(dtoPackage).append(".").append(table.createDtoName()).append(";\n");
        body.append("import ").append(dtoPackage).append(".").append(table.updateDtoName()).append(";\n");
        body.append("import ").append(dtoPackage).append(".").append(table.voName()).append(";\n");
        body.append("import ").append(repositoryPackage).append(";\n");
        body.append("import lombok.RequiredArgsConstructor;\n");
        body.append("import org.springframework.beans.BeanUtils;\n");
        body.append("import org.springframework.stereotype.Service;\n");
        body.append("import org.springframework.transaction.annotation.Transactional;\n");
        body.append("\n");
        body.append("import java.util.List;\n");
        body.append("import java.util.stream.Collectors;\n\n");

        appendClassComment(body, table.tableComment(), serviceName(table), config.author());
        body.append("@Service\n");
        body.append("@RequiredArgsConstructor\n");
        body.append("public class ").append(serviceName(table)).append(" {\n\n");
        body.append("    private final ").append(repositoryName(table)).append(" repository;\n\n");

        body.append("    public ").append(table.voName()).append(" detail(").append(pkType).append(" id) {\n");
        body.append("        ").append(table.className()).append(" entity = repository.findById(id)\n");
        body.append("                .orElseThrow(() -> new BizException(404, \"数据不存在\"));\n");
        body.append("        return toVO(entity);\n");
        body.append("    }\n\n");

        body.append("    public List<").append(table.voName()).append("> list() {\n");
        body.append("        return repository.findAll().stream()\n");
        body.append("                .map(this::toVO)\n");
        body.append("                .collect(Collectors.toList());\n");
        body.append("    }\n\n");

        body.append("    @Transactional(rollbackFor = Exception.class)\n");
        body.append("    public ").append(table.voName()).append(" create(").append(table.createDtoName()).append(" request) {\n");
        body.append("        ").append(table.className()).append(" entity = new ").append(table.className()).append("();\n");
        body.append("        BeanUtils.copyProperties(request, entity);\n");
        body.append("        return toVO(repository.save(entity));\n");
        body.append("    }\n\n");

        body.append("    @Transactional(rollbackFor = Exception.class)\n");
        body.append("    public ").append(table.voName()).append(" update(").append(pkType).append(" id, ")
                .append(table.updateDtoName()).append(" request) {\n");
        body.append("        ").append(table.className()).append(" entity = repository.findById(id)\n");
        body.append("                .orElseThrow(() -> new BizException(404, \"数据不存在\"));\n");
        body.append("        BeanUtils.copyProperties(request, entity);\n");
        body.append("        return toVO(repository.save(entity));\n");
        body.append("    }\n\n");

        body.append("    @Transactional(rollbackFor = Exception.class)\n");
        body.append("    public void delete(").append(pkType).append(" id) {\n");
        body.append("        repository.deleteById(id);\n");
        body.append("    }\n\n");

        body.append("    private ").append(table.voName()).append(" toVO(").append(table.className()).append(" entity) {\n");
        body.append("        ").append(table.voName()).append(" vo = new ").append(table.voName()).append("();\n");
        body.append("        BeanUtils.copyProperties(entity, vo);\n");
        body.append("        return vo;\n");
        body.append("    }\n");

        body.append("}\n");

        CodegenSupport.writeJavaFile(config, packageName, serviceName(table), body.toString());
    }

    private static void generateController(GeneratorConfig config, TableMeta table) {
        String packageName = config.basePackage() + ".controller.generated";
        String dtoPackage = config.basePackage() + ".dto.generated.jpa." + table.objectName().toLowerCase(Locale.ROOT);
        String servicePackage = config.basePackage() + ".service.generated." + serviceName(table);

        String pkType = primaryKeyType(table);
        String requestPath = config.apiPrefix() + table.endpoint();

        String content = """
                package %s;

                import %s;
                import %s.%s;
                import %s.%s;
                import %s.%s;
                import %s;
                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.tags.Tag;
                import jakarta.validation.Valid;
                import lombok.RequiredArgsConstructor;
                import org.springframework.web.bind.annotation.*;

                import java.util.List;

                @RestController
                @RequestMapping("%s")
                @RequiredArgsConstructor
                @Tag(name = "JPA-Auto-%s")
                public class %s {

                    private final %s service;

                    @GetMapping("/{id}")
                    @Operation(summary = "详情")
                    public R<%s> detail(@PathVariable %s id) {
                        return R.ok(service.detail(id));
                    }

                    @GetMapping("/list")
                    @Operation(summary = "列表")
                    public R<List<%s>> list() {
                        return R.ok(service.list());
                    }

                    @PostMapping("/create")
                    @Operation(summary = "新增")
                    public R<%s> create(@Valid @RequestBody %s request) {
                        return R.ok(service.create(request));
                    }

                    @PutMapping("/{id}")
                    @Operation(summary = "更新")
                    public R<%s> update(@PathVariable %s id,
                                        @Valid @RequestBody %s request) {
                        return R.ok(service.update(id, request));
                    }

                    @DeleteMapping("/{id}")
                    @Operation(summary = "删除")
                    public R<Void> delete(@PathVariable %s id) {
                        service.delete(id);
                        return R.ok();
                    }
                }
                """.formatted(
                packageName,
                config.basePackage() + ".common.result.R",
                dtoPackage, table.createDtoName(),
                dtoPackage, table.updateDtoName(),
                dtoPackage, table.voName(),
                servicePackage,
                requestPath,
                table.className(),
                controllerName(table),
                serviceName(table),
                table.voName(), pkType,
                table.voName(),
                table.voName(), table.createDtoName(),
                table.voName(), pkType, table.updateDtoName(),
                pkType
        );

        CodegenSupport.writeJavaFile(config, packageName, controllerName(table), content);
    }

    private static String buildRequestDto(String packageName,
                                          String className,
                                          List<ColumnMeta> columns,
                                          boolean includeValidation,
                                          TableMeta table,
                                          String author) {
        Set<String> imports = new LinkedHashSet<>();
        imports.add("lombok.Data");
        imports.addAll(resolveColumnImports(columns));

        boolean hasNotNull = false;
        boolean hasNotBlank = false;

        List<String> fieldLines = new ArrayList<>();
        for (ColumnMeta column : columns) {
            StringBuilder field = new StringBuilder();
            appendFieldComment(field, column.comment());

            if (includeValidation && !column.nullable()) {
                if ("String".equals(column.javaType())) {
                    field.append("    @jakarta.validation.constraints.NotBlank(message = \"")
                            .append(column.fieldName())
                            .append("不能为空\")\n");
                    hasNotBlank = true;
                } else {
                    field.append("    @jakarta.validation.constraints.NotNull(message = \"")
                            .append(column.fieldName())
                            .append("不能为空\")\n");
                    hasNotNull = true;
                }
            }

            field.append("    private ")
                    .append(column.javaType())
                    .append(" ")
                    .append(column.fieldName())
                    .append(";\n");
            fieldLines.add(field.toString());
        }

        if (hasNotNull) {
            imports.add("jakarta.validation.constraints.NotNull");
        }
        if (hasNotBlank) {
            imports.add("jakarta.validation.constraints.NotBlank");
        }

        StringBuilder body = new StringBuilder();
        body.append("package ").append(packageName).append(";\n\n");
        imports.forEach(importLine -> body.append("import ").append(importLine).append(";\n"));
        body.append("\n");

        appendClassComment(body, table.tableComment(), className, author);
        body.append("@Data\n");
        body.append("public class ").append(className).append(" {\n\n");
        for (String fieldLine : fieldLines) {
            body.append(fieldLine).append("\n");
        }
        body.append("}\n");
        return body.toString();
    }

    private static String buildVoDto(String packageName,
                                     TableMeta table,
                                     String author) {
        Set<String> imports = new LinkedHashSet<>();
        imports.add("lombok.Data");
        imports.addAll(resolveColumnImports(table.columns()));

        StringBuilder body = new StringBuilder();
        body.append("package ").append(packageName).append(";\n\n");
        imports.forEach(importLine -> body.append("import ").append(importLine).append(";\n"));
        body.append("\n");

        appendClassComment(body, table.tableComment(), table.voName(), author);
        body.append("@Data\n");
        body.append("public class ").append(table.voName()).append(" {\n\n");

        for (ColumnMeta column : table.columns()) {
            appendFieldComment(body, column.comment());
            body.append("    private ")
                    .append(column.javaType())
                    .append(" ")
                    .append(column.fieldName())
                    .append(";\n\n");
        }

        body.append("}\n");
        return body.toString();
    }

    private static String primaryKeyType(TableMeta table) {
        return table.primaryKey() == null ? "Long" : table.primaryKey().javaType();
    }

    private static String repositoryName(TableMeta table) {
        return table.className() + "JpaAutoRepository";
    }

    private static String serviceName(TableMeta table) {
        return table.className() + "JpaAutoService";
    }

    private static String controllerName(TableMeta table) {
        return table.className() + "JpaAutoController";
    }

    private static Set<String> resolveColumnImports(List<ColumnMeta> columns) {
        Set<String> imports = new LinkedHashSet<>();
        for (ColumnMeta column : columns) {
            if ("BigDecimal".equals(column.javaType())) {
                imports.add("java.math.BigDecimal");
            }
            if ("LocalDateTime".equals(column.javaType())) {
                imports.add("java.time.LocalDateTime");
            }
            if ("LocalDate".equals(column.javaType())) {
                imports.add("java.time.LocalDate");
            }
            if ("LocalTime".equals(column.javaType())) {
                imports.add("java.time.LocalTime");
            }
        }
        return imports;
    }

    private static void appendClassComment(StringBuilder body,
                                           String tableComment,
                                           String className,
                                           String author) {
        body.append("/**\n");
        if (tableComment != null && !tableComment.isBlank()) {
            body.append(" * ").append(tableComment).append("\n");
        } else {
            body.append(" * Auto generated for ").append(className).append("\n");
        }
        body.append(" * @author ").append(author).append("\n");
        body.append(" */\n");
    }

    private static void appendFieldComment(StringBuilder body, String comment) {
        if (comment != null && !comment.isBlank()) {
            body.append("    /** ").append(comment).append(" */\n");
        }
    }
}
