package com.example.ckyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CkycUpdateRequest {

    @NotBlank(message = "CKYC_NO is mandatory")
    private String ckycNo;

    @NotEmpty(message = "At least one changed field is required")
    private Map<String, String> changedFields = new LinkedHashMap<>();
}
