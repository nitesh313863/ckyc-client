package com.example.ckyc.controller;

import com.example.ckyc.constant.ApplicationConstant;
import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.dto.CkycUpdateResponseDto;
import com.example.ckyc.exception.CkycEncryptionException;
import com.example.ckyc.exception.CkycSignatureException;
import com.example.ckyc.exception.CkycUpstreamException;
import com.example.ckyc.exception.CkycValidationException;
import com.example.ckyc.service.UpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ckyc")
@Slf4j
@RequiredArgsConstructor
public class UpdateController {

    private final UpdateService updateService;

    @PostMapping("/update")
    public ResponseEntity<CkycUpdateResponseDto> update(
            @Valid @RequestBody CkycUpdateRequestDto request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getFieldErrors().isEmpty()
                    ? ApplicationConstant.Error.CKYC_VALIDATION_ERROR_MESSAGE
                    : bindingResult.getFieldErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(errorResponse(
                    request,
                    "FAILED",
                    message,
                    ApplicationConstant.Error.CKYC_VALIDATION_ERROR_CODE
            ));
        }
        try {
            return ResponseEntity.ok(updateService.update(request));
        } catch (CkycValidationException ex) {
            return ResponseEntity.badRequest().body(errorResponse(
                    request,
                    "FAILED",
                    ex.getMessage(),
                    ApplicationConstant.Error.CKYC_VALIDATION_ERROR_CODE
            ));
        } catch (CkycEncryptionException ex) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse(
                    request,
                    "FAILED",
                    ex.getMessage(),
                    ApplicationConstant.Error.CKYC_ENCRYPTION_ERROR_CODE
            ));
        } catch (CkycSignatureException ex) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse(
                    request,
                    "FAILED",
                    ex.getMessage(),
                    ApplicationConstant.Error.CKYC_SIGNATURE_ERROR_CODE
            ));
        } catch (CkycUpstreamException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse(
                    request,
                    "FAILED",
                    ex.getMessage(),
                    ApplicationConstant.Error.CKYC_UPSTREAM_ERROR_CODE
            ));
        }
    }

    private CkycUpdateResponseDto errorResponse(
            CkycUpdateRequestDto request,
            String status,
            String message,
            String errorCode
    ) {
        return CkycUpdateResponseDto.builder()
                .status(status)
                .message(message)
                .ckycNo(request == null ? null : request.getCkycNo())
                .updateType(request == null ? null : request.getUpdateType())
                .errorCode(errorCode)
                .build();
    }
}
