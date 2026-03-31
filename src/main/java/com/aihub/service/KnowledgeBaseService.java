package com.aihub.service;

import com.aihub.common.enums.ResourceTypeEnum;
import com.aihub.common.exception.BizException;
import com.aihub.dto.knowledge.CreateKnowledgeBaseRequest;
import com.aihub.dto.knowledge.KnowledgeBaseVO;
import com.aihub.entity.KnowledgeBase;
import com.aihub.repository.KnowledgeBaseRepository;
import com.aihub.security.SecurityUtils;
import com.aihub.dify.service.DifyKnowledgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final ResourceAuthService resourceAuthService;
    private final DifyKnowledgeService difyKnowledgeService;
    private final ObjectMapper objectMapper;

    public List<KnowledgeBaseVO> listByPermission(Long userId, String roleCode) {
        List<Long> authorizedIds = resourceAuthService
                .getAuthorizedResourceIds(userId, roleCode, ResourceTypeEnum.KNOWLEDGE);

        List<KnowledgeBase> list;
        if (authorizedIds == null) {
            list = knowledgeBaseRepository.findAllByDeletedFalseAndEnabledTrueOrderBySortAsc();
        } else if (authorizedIds.isEmpty()) {
            return List.of();
        } else {
            list = knowledgeBaseRepository.findAllByIdInAndDeletedFalseAndEnabledTrue(authorizedIds);
        }

        return list.stream().map(this::toVO).toList();
    }

    public KnowledgeBaseVO getDetail(Long id) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "知识库不存在"));
        return toVO(kb);
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO create(CreateKnowledgeBaseRequest request) {
        String datasetId = request.getDifyDatasetId();
        if (datasetId == null || datasetId.isBlank()) {
            Map<String, Object> body = new HashMap<>();
            body.put("name", request.getKbName());
            if (request.getDescription() != null && !request.getDescription().isBlank()) {
                body.put("description", request.getDescription());
            }
            if (request.getIndexingTechnique() != null && !request.getIndexingTechnique().isBlank()) {
                body.put("indexing_technique", request.getIndexingTechnique());
            }
            Object created = difyKnowledgeService.createDataset(
                    request.getDifyApiKey(), request.getDifyBaseUrl(), body);
            datasetId = extractDatasetId(created);
            if (datasetId == null || datasetId.isBlank()) {
                throw new BizException(500, "创建 Dify 知识库失败，未返回 datasetId");
            }
        }

        KnowledgeBase kb = new KnowledgeBase();
        kb.setKbName(request.getKbName());
        kb.setKbCode(request.getKbCode());
        kb.setDifyDatasetId(datasetId);
        kb.setDifyApiKey(request.getDifyApiKey());
        kb.setDifyBaseUrl(request.getDifyBaseUrl());
        kb.setDescription(request.getDescription());
        kb.setIcon(request.getIcon());
        kb.setIndexingTechnique(request.getIndexingTechnique());
        kb.setEmbeddingModel(request.getEmbeddingModel());
        kb.setSort(request.getSort() != null ? request.getSort() : 0);
        kb.setEnabled(true);
        kb.setDeleted(false);
        kb.setDocumentCount(0);
        kb.setWordCount(0L);
        kb.setCreatedBy(SecurityUtils.getCurrentUserId());
        return toVO(knowledgeBaseRepository.save(kb));
    }

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO update(Long id, CreateKnowledgeBaseRequest request) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "知识库不存在"));

        String apiKey = request.getDifyApiKey() != null && !request.getDifyApiKey().isBlank()
                ? request.getDifyApiKey() : kb.getDifyApiKey();
        String baseUrl = request.getDifyBaseUrl() != null && !request.getDifyBaseUrl().isBlank()
                ? request.getDifyBaseUrl() : kb.getDifyBaseUrl();

        Map<String, Object> body = new HashMap<>();
        body.put("name", request.getKbName());
        if (request.getDescription() != null) {
            body.put("description", request.getDescription());
        }

        difyKnowledgeService.updateDataset(kb.getDifyDatasetId(), apiKey, baseUrl, body);

        kb.setKbName(request.getKbName());
        kb.setKbCode(request.getKbCode());
        kb.setDifyApiKey(apiKey);
        kb.setDifyBaseUrl(baseUrl);
        kb.setDescription(request.getDescription());
        kb.setIcon(request.getIcon());
        kb.setIndexingTechnique(request.getIndexingTechnique());
        kb.setEmbeddingModel(request.getEmbeddingModel());
        if (request.getSort() != null) {
            kb.setSort(request.getSort());
        }

        return toVO(knowledgeBaseRepository.save(kb));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "知识库不存在"));
        difyKnowledgeService.deleteDataset(kb.getDifyDatasetId(), kb.getDifyApiKey(), kb.getDifyBaseUrl());
        kb.setDeleted(true);
        knowledgeBaseRepository.save(kb);
    }

    private String extractDatasetId(Object created) {
        Map<?, ?> map = objectMapper.convertValue(created, Map.class);
        Object idValue = map.get("id");
        if (idValue != null) {
            return String.valueOf(idValue);
        }
        Object data = map.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object nestedId = dataMap.get("id");
            if (nestedId != null) {
                return String.valueOf(nestedId);
            }
            Object nestedDatasetId = dataMap.get("dataset_id");
            if (nestedDatasetId != null) {
                return String.valueOf(nestedDatasetId);
            }
        }
        Object datasetId = map.get("dataset_id");
        return datasetId != null ? String.valueOf(datasetId) : null;
    }

    private KnowledgeBaseVO toVO(KnowledgeBase kb) {
        return KnowledgeBaseVO.builder()
                .id(kb.getId())
                .kbName(kb.getKbName())
                .kbCode(kb.getKbCode())
                .description(kb.getDescription())
                .icon(kb.getIcon())
                .documentCount(kb.getDocumentCount())
                .wordCount(kb.getWordCount())
                .indexingTechnique(kb.getIndexingTechnique())
                .embeddingModel(kb.getEmbeddingModel())
                .sort(kb.getSort())
                .enabled(kb.getEnabled())
                .createdAt(kb.getCreatedAt())
                .build();
    }
}