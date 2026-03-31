package com.aihub.controller;

import com.aihub.annotation.RequireResource;
import com.aihub.common.enums.ResourceTypeEnum;
import com.aihub.common.result.R;
import com.aihub.dify.service.DifyKnowledgeService;
import com.aihub.dto.knowledge.CreateKnowledgeBaseRequest;
import com.aihub.dto.knowledge.KnowledgeBaseVO;
import com.aihub.security.SecurityUtils;
import com.aihub.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DifyKnowledgeService difyKnowledgeService;

    @GetMapping("/list")
    @Operation(summary = "获取知识库列表（按权限过滤）")
    public R<List<KnowledgeBaseVO>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        String roleCode = SecurityUtils.getCurrentRoleCode();
        return R.ok(knowledgeBaseService.listByPermission(userId, roleCode));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情")
    @RequireResource(type = ResourceTypeEnum.KNOWLEDGE, paramName = "id")
    public R<KnowledgeBaseVO> detail(@PathVariable Long id) {
        return R.ok(knowledgeBaseService.getDetail(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建知识库（管理员）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<KnowledgeBaseVO> create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return R.ok(knowledgeBaseService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新知识库（管理员）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<KnowledgeBaseVO> update(@PathVariable Long id,
                                     @Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return R.ok(knowledgeBaseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库（管理员）")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/documents")
    @Operation(summary = "获取文档列表")
    @RequireResource(type = ResourceTypeEnum.KNOWLEDGE, paramName = "id")
    public R<Object> listDocuments(@PathVariable Long id,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "20") Integer limit) {
        return R.ok(difyKnowledgeService.listDocuments(id, keyword, page, limit));
    }

    @PostMapping("/{id}/documents/text")
    @Operation(summary = "通过文本创建文档")
    @RequireResource(type = ResourceTypeEnum.KNOWLEDGE, paramName = "id")
    public R<Object> createDocumentByText(@PathVariable Long id,
                                          @RequestBody Map<String, Object> request) {
        return R.ok(difyKnowledgeService.createDocumentByText(id, request));
    }

    @PostMapping("/{id}/documents/file")
    @Operation(summary = "通过文件创建文档")
    @RequireResource(type = ResourceTypeEnum.KNOWLEDGE, paramName = "id")
    public R<Object> createDocumentByFile(@PathVariable Long id,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) String processRule) {
        return R.ok(difyKnowledgeService.createDocumentByFile(id, file, processRule));
    }

    @PutMapping("/{id}/documents/{docId}/text")
    @Operation(summary = "更新文档（文本方式）")
    @RequireResource(type = ResourceTypeEnum.KNOWLEDGE, paramName = "id")
    public R<Object> updateDocumentByText(@PathVariable Long id,
                                          @PathVariable String docId,
                                          @RequestBody Map<String, Object> request) {
        return R.ok(difyKnowledgeService.updateDocumentByText(id, docId, request));
    }

    @PutMapping("/{id}/documents/{docId}/file")
    @Operation(summary = "更新文档（文件方式）")
    @RequireResource(type = ResourceTypeEnum.KNOWLEDGE, paramName = "id")
    public R<Object> updateDocumentByFile(@PathVariable Long id,
                                          @PathVariable String docId,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) String processRule) {
        return R.ok(difyKnowledgeService.updateDocumentByFile(id, docId, file, processRule));
    }

    @DeleteMapping("/{id}/documents/{docId}")
    @Operation(summary = "删除文档")
    @RequireResource(type = ResourceTypeEnum.KNOWLEDGE, paramName = "id")
    public R<Object> deleteDocument(@PathVariable Long id,
                                    @PathVariable String docId) {
        return R.ok(difyKnowledgeService.deleteDocument(id, docId));
    }

    @GetMapping("/{id}/documents/indexing-status")
    @Operation(summary = "获取文档索引状态")
    @RequireResource(type = ResourceTypeEnum.KNOWLEDGE, paramName = "id")
    public R<Object> indexingStatus(@PathVariable Long id,
                                    @RequestParam String batch) {
        return R.ok(difyKnowledgeService.getIndexingStatus(id, batch));
    }

    @GetMapping("/{id}/documents/{docId}/segments")
    @Operation(summary = "获取文档分段")
    @RequireResource(type = ResourceTypeEnum.KNOWLEDGE, paramName = "id")
    public R<Object> listSegments(@PathVariable Long id,
                                  @PathVariable String docId) {
        return R.ok(difyKnowledgeService.listSegments(id, docId));
    }

    @PostMapping("/{id}/retrieve")
    @Operation(summary = "知识库检索测试")
    @RequireResource(type = ResourceTypeEnum.KNOWLEDGE, paramName = "id")
    public R<Object> retrieve(@PathVariable Long id,
                              @RequestBody Map<String, Object> request) {
        return R.ok(difyKnowledgeService.retrieve(id, request));
    }
}