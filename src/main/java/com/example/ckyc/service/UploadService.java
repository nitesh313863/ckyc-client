package com.example.ckyc.service;

import com.example.ckyc.dto.CkycApiResponsePayload;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.model.ApiResponse;

public interface UploadService {
    ApiResponse<CkycApiResponsePayload> upload(CkycUploadRequest request);
}
