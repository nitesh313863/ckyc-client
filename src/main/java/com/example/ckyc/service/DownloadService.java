package com.example.ckyc.service;

import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.model.ApiResponse;

import java.util.Map;

public interface DownloadService {
    ApiResponse<Map<String, Object>> download(CkycDownloadRequest request);
    ApiResponse<Map<String, Object>> validateOtp(CkycValidateOtpRequest request);
}
