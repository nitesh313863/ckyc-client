package com.example.ckyc.dto;

import com.example.ckyc.constant.AuthFactorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CkycDownloadRequest {

    @NotBlank(message = "CKYC_NO is mandatory")
    private String ckycNo;

    @NotNull(message = "AUTH_FACTOR_TYPE is mandatory")
    private AuthFactorType authFactorType;

    @NotBlank(message = "AUTH_FACTOR is mandatory")
    private String authFactor;
}