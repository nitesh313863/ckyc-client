package com.example.ckyc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CkycDownloadRequest {

    @NotBlank(message = "CKYC_NO is mandatory")
    private String ckycNo;

    @NotBlank(message = "AUTH_FACTOR_TYPE is mandatory")
    private String authFactorType;

    @NotBlank(message = "AUTH_FACTOR is mandatory")
    private String authFactor;
}
