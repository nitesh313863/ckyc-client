package com.example.ckyc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CkycResponseDto {
    private String operation;
    private String rootTag;
    private Boolean signatureValid;

    private String fiCode;
    private String requestId;
    private String reqDate;
    private String version;

    private String status;
    private String ckycNo;
    private String ckycReferenceId;
    private String errorCode;
    private String errorDesc;
    private String error;
    private Boolean otpRequired;

    private String decryptedPidData;
    private Map<String, Object> pidDetails;
    private Map<String, String> pidFlat;
    private CkycSearchResponseDto searchResponse;
    private CkycDownloadResponseDto downloadResponse;
}
