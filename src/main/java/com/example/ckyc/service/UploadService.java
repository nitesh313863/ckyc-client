package com.example.ckyc.service;

import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.model.ApiResponse;

import java.util.Map;

public interface UploadService {
    ApiResponse<Map<String, Object>> upload(CkycUploadRequest request);
}
