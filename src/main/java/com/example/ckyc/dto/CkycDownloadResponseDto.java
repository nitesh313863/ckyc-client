package com.example.ckyc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CkycDownloadResponseDto {
    private Map<String, Object> recordCountDetails;
    private Map<String, Object> personalDetails;
    private Map<String, Object> addressDetails;
    private List<Map<String, Object>> identityDetails;
    private List<Map<String, Object>> relatedPersons;
    private List<Map<String, Object>> images;
    private Map<String, Object> legalEntityDetails;
    private Map<String, Object> otherDetails;
}
