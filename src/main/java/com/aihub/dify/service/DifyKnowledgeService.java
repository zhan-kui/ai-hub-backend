package com.aihub.dify.service;

import com.aihub.common.exception.BizException;
import com.aihub.config.DifyProperties;
import com.aihub.entity.KnowledgeBase;
import com.aihub.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.MultipartBodyBuilder;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DifyKnowledgeService {

    private final WebClient webClient;
    private final DifyProperties difyProperties;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public Object createDataset(String apiKey, String baseUrl, Map<String, Object> request) {
        return webClient.mutate().baseUrl(resolveBaseUrl(baseUrl)).build()
                .post()
                .uri("/datasets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object updateDataset(String datasetId, String apiKey,
                                String baseUrl, Map<String, Object> request) {
        return webClient.mutate().baseUrl(resolveBaseUrl(baseUrl)).build()
                .patch()
                .uri("/datasets/{datasetId}", datasetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public void deleteDataset(String datasetId, String apiKey, String baseUrl) {
        webClient.mutate().baseUrl(resolveBaseUrl(baseUrl)).build()
                .delete()
                .uri("/datasets/{datasetId}", datasetId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public Object listDocuments(Long kbId, String keyword, Integer page, Integer limit) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        String baseUrl = resolveBaseUrl(kb);

        return webClient.mutate().baseUrl(baseUrl).build()
                .get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/datasets/{datasetId}/documents")
                            .queryParam("page", page)
                            .queryParam("limit", limit);
                    if (keyword != null && !keyword.isBlank()) {
                        uriBuilder.queryParam("keyword", keyword);
                    }
                    return uriBuilder.build(kb.getDifyDatasetId());
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kb.getDifyApiKey())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object createDocumentByText(Long kbId, Map<String, Object> request) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        String baseUrl = resolveBaseUrl(kb);

        return webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/datasets/{datasetId}/documents/create_by_text", kb.getDifyDatasetId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kb.getDifyApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object createDocumentByFile(Long kbId, MultipartFile file, String processRule) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        String baseUrl = resolveBaseUrl(kb);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource());
        if (processRule != null && !processRule.isBlank()) {
            bodyBuilder.part("process_rule", processRule);
        }

        return webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/datasets/{datasetId}/documents/create_by_file", kb.getDifyDatasetId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kb.getDifyApiKey())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object updateDocumentByText(Long kbId, String docId, Map<String, Object> request) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        String baseUrl = resolveBaseUrl(kb);

        return webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/datasets/{datasetId}/documents/{docId}/update_by_text", kb.getDifyDatasetId(), docId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kb.getDifyApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object updateDocumentByFile(Long kbId, String docId, MultipartFile file, String processRule) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        String baseUrl = resolveBaseUrl(kb);

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", file.getResource());
        if (processRule != null && !processRule.isBlank()) {
            bodyBuilder.part("process_rule", processRule);
        }

        return webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/datasets/{datasetId}/documents/{docId}/update_by_file", kb.getDifyDatasetId(), docId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kb.getDifyApiKey())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(bodyBuilder.build())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object deleteDocument(Long kbId, String docId) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        String baseUrl = resolveBaseUrl(kb);

        return webClient.mutate().baseUrl(baseUrl).build()
                .delete()
                .uri("/datasets/{datasetId}/documents/{docId}", kb.getDifyDatasetId(), docId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kb.getDifyApiKey())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object getIndexingStatus(Long kbId, String batch) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        String baseUrl = resolveBaseUrl(kb);

        return webClient.mutate().baseUrl(baseUrl).build()
                .get()
                .uri("/datasets/{datasetId}/documents/{batch}/indexing-status", kb.getDifyDatasetId(), batch)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kb.getDifyApiKey())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object listSegments(Long kbId, String docId) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        String baseUrl = resolveBaseUrl(kb);

        return webClient.mutate().baseUrl(baseUrl).build()
                .get()
                .uri("/datasets/{datasetId}/documents/{docId}/segments", kb.getDifyDatasetId(), docId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kb.getDifyApiKey())
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    public Object retrieve(Long kbId, Map<String, Object> request) {
        KnowledgeBase kb = getKnowledgeBase(kbId);
        String baseUrl = resolveBaseUrl(kb);

        return webClient.mutate().baseUrl(baseUrl).build()
                .post()
                .uri("/datasets/{datasetId}/retrieve", kb.getDifyDatasetId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + kb.getDifyApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    private KnowledgeBase getKnowledgeBase(Long kbId) {
        return knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BizException(404, "知识库不存在"));
    }

    private String resolveBaseUrl(KnowledgeBase kb) {
        return resolveBaseUrl(kb.getDifyBaseUrl());
    }

    private String resolveBaseUrl(String baseUrl) {
        return (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : difyProperties.getBaseUrl();
    }
}