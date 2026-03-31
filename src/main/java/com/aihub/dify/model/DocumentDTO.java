package com.aihub.dify.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentDTO {

    private String id;

    private String name;

    private String indexingStatus;

    private Integer wordCount;

    private String createdBy;
}