package com.example.ckyc.service;

import com.example.ckyc.dto.CkycApiResponsePayload;
import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.dto.CkycUpdateResponseDto;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.model.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;

public interface CkycControllerService {
    CkycApiResponsePayload search(String idType, String idNo);

    ApiResponse<CkycApiResponsePayload> download(CkycDownloadRequest request);

    ApiResponse<CkycApiResponsePayload> validateOtp(CkycValidateOtpRequest request);

    ApiResponse<CkycApiResponsePayload> upload(CkycUploadRequest request);

    ResponseEntity<CkycUpdateResponseDto> update(CkycUpdateRequestDto request, BindingResult bindingResult);
}
