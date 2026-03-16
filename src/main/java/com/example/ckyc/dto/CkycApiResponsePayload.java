package com.example.ckyc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CkycApiResponsePayload {
    private String requestXml;
    private String rawResponseXml;
    private CkycResponseDto response;
}
