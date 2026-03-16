package com.example.ckyc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CkycSearchResponseDto {
    private String ckycNo;
    private String ckycReferenceId;
    private String name;
    private String fathersName;
    private String age;
    private String imageType;
    private String photo;
    private String kycDate;
    private String updatedDate;
    private String remarks;
    private List<SearchIdStatus> idList;
    private Map<String, Object> additionalFields;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SearchIdStatus {
        private String type;
        private String status;
    }
}
