package com.example.ckyc.controller;

import com.example.ckyc.dto.CkycApiResponsePayload;
import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.dto.CkycUpdateResponseDto;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.model.ApiResponse;
import com.example.ckyc.service.CkycControllerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ckyc")
@RequiredArgsConstructor
public class CkycController {

    private final CkycControllerService ckycControllerService;

    @PostMapping("/search")
    public ResponseEntity<CkycApiResponsePayload> search(@RequestParam String idType,
                                                         @RequestParam String idNo) {
        return ResponseEntity.ok(ckycControllerService.search(idType, idNo));
    }

    @PostMapping("/download")
    public ResponseEntity<ApiResponse<CkycApiResponsePayload>> download(@Valid @RequestBody CkycDownloadRequest request) {
        return ResponseEntity.ok(ckycControllerService.download(request));
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<ApiResponse<CkycApiResponsePayload>> validateOtp(
            @Valid @RequestBody CkycValidateOtpRequest request
    ) {
        return ResponseEntity.ok(ckycControllerService.validateOtp(request));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<CkycApiResponsePayload>> upload(@Valid @RequestBody CkycUploadRequest request) {
        return ResponseEntity.ok(ckycControllerService.upload(request));
    }

    @PostMapping("/update")
    public ResponseEntity<CkycUpdateResponseDto> update(
            @Valid @RequestBody CkycUpdateRequestDto request,
            BindingResult bindingResult
    ) {
        return ckycControllerService.update(request, bindingResult);
    }
}
