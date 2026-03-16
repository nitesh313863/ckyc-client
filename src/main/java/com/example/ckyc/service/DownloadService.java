package com.example.ckyc.service;

import com.example.ckyc.dto.CkycApiResponsePayload;
import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.model.ApiResponse;

public interface DownloadService {
    ApiResponse<CkycApiResponsePayload> download(CkycDownloadRequest request);
    ApiResponse<CkycApiResponsePayload> validateOtp(CkycValidateOtpRequest request);
}
