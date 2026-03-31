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

public class MybatisPlusCodeGenerator {

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
            generateMapper(config, table);
            generateMapperXml(config, table);
            generateDto(config, table);
            generateService(config, table);
            generateController(config, table);
        }

        System.out.println("MyBatis-Plus scaffold generation finished. tables=" + tables.size());
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
                "/gen/mp"
        );
    }

    private static void generateEntity(GeneratorConfig config, TableMeta table) {
        String packageName = config.basePackage() + ".entity";
        Set<String> imports = new LinkedHashSet<>();
        imports.add("com.baomidou.mybatisplus.annotation.IdType");
        imports.add("com.baomidou.mybatisplus.annotation.TableField");
        imports.add("com.baomidou.mybatisplus.annotation.TableId");
        imports.add("com.baomidou.mybatisplus.annotation.TableName");
        imports.add("jakarta.persistence.Column");
        imports.add("jakarta.persistence.Entity");
        imports.add("jakarta.persistence.Id");
        imports.add("jakarta.persistence.Table");
        imports.add("lombok.Data");
        if (table.columns().stream().anyMatch(column -> "deleted".equalsIgnoreCase(column.columnName()))) {
            imports.add("com.baomidou.mybatisplus.annotation.TableLogic");
        }
        imports.addAll(CodegenSupport.collectCommonImports(table));

        StringBuilder body = new StringBuilder();
        body.append("package ").append(packageName).append(";\n\n");
        imports.forEach(importLine -> body.append("import ").append(importLine).append(";\n"));
        body.append("\n");

        appendClassComment(body, table.tableComment(), table.className(), config.author());
        body.append("@Data\n");
        body.append("@Entity\n");
        body.append("@Table(name = \"").append(table.tableName()).append("\")\n");
        body.append("@TableName(\"").append(table.tableName()).append("\")\n");
        body.append("public class ").append(table.className()).append(" {\n\n");

        for (ColumnMeta column : table.columns()) {
            appendFieldComment(body, column.comment());
            if (column.primaryKey()) {
                body.append("    @Id\n");
                body.append("    @TableId(type = ")
                        .append(column.autoIncrement() ? "IdType.AUTO" : "IdType.INPUT")
                        .append(")\n");
            } else {
                body.append("    @TableField(\"").append(column.columnName()).append("\")\n");
            }

            body.append("    @Column(name = \"").append(column.columnName()).append("\"");
            if (!column.nullable()) {
                body.append(", nullable = false");
            }
            if ("String".equals(column.javaType()) && column.size() > 0 && column.size() <= 65535) {
                body.append(", length = ").append(column.size());
            }
            body.append(")\n");

            if ("deleted".equalsIgnoreCase(column.columnName())) {
                body.append("    @TableLogic\n");
            }

            body.append("    private ")
                    .append(column.javaType())
                    .append(" ")
                    .append(column.fieldName())
                    .append(";\n\n");
        }

        body.append("}\n");

        CodegenSupport.writeJavaFile(config, packageName, table.className(), body.toString());
    }

    private static void generateMapper(GeneratorConfig config, TableMeta table) {
        String packageName = config.basePackage() + ".mapper";
        String entityPackage = config.basePackage() + ".entity." + table.className();

        String content = """
                package %s;

                import %s;
                import com.baomidou.mybatisplus.core.mapper.BaseMapper;

                public interface %s extends BaseMapper<%s> {
                }
                """.formatted(packageName, entityPackage, table.mapperName(), table.className());

        CodegenSupport.writeJavaFile(config, packageName, table.mapperName(), content);
    }

    private static void generateMapperXml(GeneratorConfig config, TableMeta table) {
        StringBuilder xml = new StringBuilder();
        String mapperNamespace = config.basePackage() + ".mapper." + table.mapperName();
        String entityName = config.basePackage() + ".entity." + table.className();

        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n");
        xml.append("<mapper namespace=\"").append(mapperNamespace).append("\">\n\n");

        xml.append("    <resultMap id=\"BaseResultMap\" type=\"").append(entityName).append("\">\n");
        for (ColumnMeta column : table.columns()) {
            String tag = column.primaryKey() ? "id" : "result";
            xml.append("        <")
                    .append(tag)
                    .append(" column=\"").append(column.columnName())
                    .append("\" property=\"").append(column.fieldName())
                    .append("\" jdbcType=\"").append(column.jdbcTypeName())
                    .append("\" />\n");
        }
        xml.append("    </resultMap>\n\n");

        xml.append("    <sql id=\"Base_Column_List\">\n");
        xml.append("        ");
        for (int index = 0; index < table.columns().size(); index++) {
            xml.append(table.columns().get(index).columnName());
            if (index < table.columns().size() - 1) {
                xml.append(", ");
            }
        }
        xml.append("\n    </sql>\n\n");

        xml.append("</mapper>\n");

        CodegenSupport.writeResourceFile(config, "mapper/" + table.mapperName() + ".xml", xml.toString());
    }

    private static void generateDto(GeneratorConfig config, TableMeta table) {
        String dtoPackage = config.basePackage() + ".dto.generated." + table.objectName().toLowerCase(Locale.ROOT);

        CodegenSupport.writeJavaFile(config, dtoPackage, table.createDtoName(),
                buildRequestDto(dtoPackage, table.createDtoName(), table.insertableColumns(), true, table, config.author()));

        CodegenSupport.writeJavaFile(config, dtoPackage, table.updateDtoName(),
                buildRequestDto(dtoPackage, table.updateDtoName(), table.updatableColumns(), false, table, config.author()));

        CodegenSupport.writeJavaFile(config, dtoPackage, table.voName(),
                buildVoDto(dtoPackage, table, config.author()));
    }

    private static void generateService(GeneratorConfig config, TableMeta table) {
        String packageName = config.basePackage() + ".service.generated";
        String dtoPackage = config.basePackage() + ".dto.generated." + table.objectName().toLowerCase(Locale.ROOT);

        String pkType = primaryKeyType(table);
        Set<String> imports = new LinkedHashSet<>();
        imports.add(config.basePackage() + ".common.exception.BizException");
        imports.add(config.basePackage() + ".entity." + table.className());
        imports.add(config.basePackage() + ".mapper." + table.mapperName());
        imports.add(dtoPackage + "." + table.createDtoName());
        imports.add(dtoPackage + "." + table.updateDtoName());
        imports.add(dtoPackage + "." + table.voName());
        imports.add("lombok.RequiredArgsConstructor");
        imports.add("org.springframework.beans.BeanUtils");
        imports.add("org.springframework.stereotype.Service");
        imports.add("org.springframework.transaction.annotation.Transactional");
        imports.add("java.util.List");
        imports.add("java.util.stream.Collectors");

        StringBuilder body = new StringBuilder();
        body.append("package ").append(packageName).append(";\n\n");
        imports.forEach(importLine -> body.append("import ").append(importLine).append(";\n"));
        body.append("\n");
        appendClassComment(body, table.tableComment(), table.serviceName(), config.author());

        body.append("@Service\n");
        body.append("@RequiredArgsConstructor\n");
        body.append("public class ").append(table.serviceName()).append(" {\n\n");
        body.append("    private final ").append(table.mapperName()).append(" mapper;\n\n");

        body.append("    public ").append(table.voName()).append(" detail(").append(pkType).append(" id) {\n");
        body.append("        ").append(table.className()).append(" entity = mapper.selectById(id);\n");
        body.append("        if (entity == null) {\n");
        body.append("            throw new BizException(404, \"数据不存在\");\n");
        body.append("        }\n");
        body.append("        return toVO(entity);\n");
        body.append("    }\n\n");

        body.append("    public List<").append(table.voName()).append("> list() {\n");
        body.append("        return mapper.selectList(null).stream()\n");
        body.append("                .map(this::toVO)\n");
        body.append("                .collect(Collectors.toList());\n");
        body.append("    }\n\n");

        body.append("    @Transactional(rollbackFor = Exception.class)\n");
        body.append("    public ").append(table.voName()).append(" create(").append(table.createDtoName()).append(" request) {\n");
        body.append("        ").append(table.className()).append(" entity = new ").append(table.className()).append("();\n");
        body.append("        BeanUtils.copyProperties(request, entity);\n");
        body.append("        mapper.insert(entity);\n");
        body.append("        return toVO(entity);\n");
        body.append("    }\n\n");

        body.append("    @Transactional(rollbackFor = Exception.class)\n");
        body.append("    public ").append(table.voName()).append(" update(").append(pkType).append(" id, ")
                .append(table.updateDtoName()).append(" request) {\n");
        body.append("        ").append(table.className()).append(" entity = mapper.selectById(id);\n");
        body.append("        if (entity == null) {\n");
        body.append("            throw new BizException(404, \"数据不存在\");\n");
        body.append("        }\n");
        body.append("        BeanUtils.copyProperties(request, entity);\n");
        body.append("        mapper.updateById(entity);\n");
        body.append("        return toVO(entity);\n");
        body.append("    }\n\n");

        body.append("    @Transactional(rollbackFor = Exception.class)\n");
        body.append("    public void delete(").append(pkType).append(" id) {\n");
        body.append("        mapper.deleteById(id);\n");
        body.append("    }\n\n");

        body.append("    private ").append(table.voName()).append(" toVO(").append(table.className()).append(" entity) {\n");
        body.append("        ").append(table.voName()).append(" vo = new ").append(table.voName()).append("();\n");
        body.append("        BeanUtils.copyProperties(entity, vo);\n");
        body.append("        return vo;\n");
        body.append("    }\n");

        body.append("}\n");

        CodegenSupport.writeJavaFile(config, packageName, table.serviceName(), body.toString());
    }

    private static void generateController(GeneratorConfig config, TableMeta table) {
        String packageName = config.basePackage() + ".controller.generated";
        String dtoPackage = config.basePackage() + ".dto.generated." + table.objectName().toLowerCase(Locale.ROOT);
        String servicePackage = config.basePackage() + ".service.generated." + table.serviceName();

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
                @Tag(name = "Auto-%s")
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
                table.controllerName(),
                table.serviceName(),
                table.voName(), pkType,
                table.voName(),
                table.voName(), table.createDtoName(),
                table.voName(), pkType, table.updateDtoName(),
                pkType
        );

        CodegenSupport.writeJavaFile(config, packageName, table.controllerName(), content);
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
