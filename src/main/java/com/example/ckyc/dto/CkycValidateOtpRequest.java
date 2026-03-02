package com.example.ckyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CkycValidateOtpRequest {

    @NotBlank(message = "CKYC_NO is mandatory")
    private String ckycNo;

    @NotBlank(message = "OTP is mandatory")
    @Pattern(regexp = "^\\d{4,8}$", message = "OTP must be numeric and 4 to 8 digits")
    private String otp;
}
