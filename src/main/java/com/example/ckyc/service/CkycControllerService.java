package com.example.ckyc.service;

import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.dto.CkycUpdateResponseDto;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.model.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

import java.util.Map;

public interface CkycControllerService {
    Map<String, Object> search(String idType, String idNo);

    ApiResponse<Map<String, Object>> download(CkycDownloadRequest request);

    ApiResponse<Map<String, Object>> validateOtp(CkycValidateOtpRequest request);

    ApiResponse<Map<String, Object>> upload(CkycUploadRequest request);

    ResponseEntity<CkycUpdateResponseDto> update(CkycUpdateRequestDto request, BindingResult bindingResult);
}
