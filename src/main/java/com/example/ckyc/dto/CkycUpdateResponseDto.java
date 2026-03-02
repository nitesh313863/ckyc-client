package com.example.ckyc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CkycUpdateResponseDto {
    private String status;
    private String message;
    private String ckycNo;
    private String updateType;
    private String errorCode;
}
