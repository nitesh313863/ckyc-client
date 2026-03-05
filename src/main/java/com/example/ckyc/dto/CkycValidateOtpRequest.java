package com.example.ckyc.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CkycValidateOtpRequest {

    // Optional for audit; CKYC spec validate-otp PID_DATA does not require CKYC_NO
    private String ckycNo;

    @Pattern(regexp = "^\\d{6}$", message = "OTP must be numeric 6 digits when provided")
    private String otp;

    private String validate;

    private String requestId;
}
