package com.example.ckyc.exception;

import com.example.ckyc.constant.ApplicationConstant;
import com.example.ckyc.util.MaskingUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MaskingUtil maskingUtil;

    @ExceptionHandler(CkycValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleCkycValidationException(
            CkycValidationException ex,
            HttpServletRequest request
    ) {
        log.warn("CKYC validation failure path={} message={}", request.getRequestURI(), ex.getMessage());
        return build(
                HttpStatus.BAD_REQUEST,
                ApplicationConstant.Error.CKYC_VALIDATION_ERROR_CODE,
                defaultMessage(ex.getMessage(), ApplicationConstant.Error.CKYC_VALIDATION_ERROR_MESSAGE),
                request,
                null
        );
    }

    @ExceptionHandler(CkycEncryptionException.class)
    public ResponseEntity<ApiErrorResponse> handleCkycEncryptionException(
            CkycEncryptionException ex,
            HttpServletRequest request
    ) {
        log.error("CKYC encryption/decryption failure path={}", request.getRequestURI(), ex);
        return build(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApplicationConstant.Error.CKYC_ENCRYPTION_ERROR_CODE,
                defaultMessage(ex.getMessage(), ApplicationConstant.Error.CKYC_ENCRYPTION_ERROR_MESSAGE),
                request,
                null
        );
    }

    @ExceptionHandler(CkycSignatureException.class)
    public ResponseEntity<ApiErrorResponse> handleCkycSignatureException(
            CkycSignatureException ex,
            HttpServletRequest request
    ) {
        log.error("CKYC signature failure path={} message={}", request.getRequestURI(), ex.getMessage());
        return build(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApplicationConstant.Error.CKYC_SIGNATURE_ERROR_CODE,
                defaultMessage(ex.getMessage(), ApplicationConstant.Error.CKYC_SIGNATURE_ERROR_MESSAGE),
                request,
                null
        );
    }

    @ExceptionHandler(CkycUpstreamException.class)
    public ResponseEntity<ApiErrorResponse> handleCkycUpstreamException(
            CkycUpstreamException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = ex.getUpstreamStatusCode() == 504
                ? HttpStatus.GATEWAY_TIMEOUT
                : HttpStatus.BAD_GATEWAY;
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("upstreamStatusCode", ex.getUpstreamStatusCode());
        details.put("retryable", ex.isRetryable());
        details.put("upstreamResponseBody", maskingUtil.maskSensitive(ex.getUpstreamResponseBody()));
        return build(
                status,
                ApplicationConstant.Error.CKYC_UPSTREAM_ERROR_CODE,
                defaultMessage(ex.getMessage(), ApplicationConstant.Error.CKYC_UPSTREAM_ERROR_MESSAGE),
                request,
                details
        );
    }

    @ExceptionHandler(CkycException.class)
    public ResponseEntity<ApiErrorResponse> handleCkycException(CkycException ex, HttpServletRequest request) {
        log.warn("Handled CkycException at path={} message={}", request.getRequestURI(), ex.getMessage());
        return build(
                HttpStatus.BAD_REQUEST,
                ApplicationConstant.Error.CKYC_ERROR_CODE,
                defaultMessage(ex.getMessage(), ApplicationConstant.Error.CKYC_ERROR_MESSAGE),
                request,
                null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing request parameter at path={} param={}", request.getRequestURI(), ex.getParameterName());
        String message = ApplicationConstant.Error.MISSING_PARAMETER_MESSAGE + ": " + ex.getParameterName();
        return build(HttpStatus.BAD_REQUEST, ApplicationConstant.Error.MISSING_PARAMETER_CODE, message, request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Parameter type mismatch at path={} param={}", request.getRequestURI(), ex.getName());
        String message = ApplicationConstant.Error.INVALID_PARAMETER_MESSAGE + ": " + ex.getName();
        return build(HttpStatus.BAD_REQUEST, ApplicationConstant.Error.INVALID_PARAMETER_CODE, message, request, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed request payload at path={}", request.getRequestURI());
        return build(
                HttpStatus.BAD_REQUEST,
                ApplicationConstant.Error.MALFORMED_REQUEST_CODE,
                ApplicationConstant.Error.MALFORMED_REQUEST_MESSAGE,
                request,
                ex.getMessage()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not allowed at path={} method={}", request.getRequestURI(), ex.getMethod());
        String message = ApplicationConstant.Error.METHOD_NOT_ALLOWED_MESSAGE + ": " + ex.getMethod();
        return build(
                HttpStatus.METHOD_NOT_ALLOWED,
                ApplicationConstant.Error.METHOD_NOT_ALLOWED_CODE,
                message,
                request,
                ex.getSupportedMethods()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Unsupported media type at path={} contentType={}", request.getRequestURI(), ex.getContentType());
        return build(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ApplicationConstant.Error.UNSUPPORTED_MEDIA_TYPE_CODE,
                ApplicationConstant.Error.UNSUPPORTED_MEDIA_TYPE_MESSAGE,
                request,
                ex.getSupportedMediaTypes()
        );
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiErrorResponse> handleRestClientResponse(
            RestClientResponseException ex, HttpServletRequest request) {
        log.error("CKYC upstream error at path={} upstreamStatus={}", request.getRequestURI(), ex.getRawStatusCode());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("upstreamStatusCode", ex.getRawStatusCode());
        details.put("upstreamResponseBody", maskingUtil.maskSensitive(ex.getResponseBodyAsString()));
        return build(
                HttpStatus.BAD_GATEWAY,
                ApplicationConstant.Error.CKYC_UPSTREAM_ERROR_CODE,
                ApplicationConstant.Error.CKYC_UPSTREAM_ERROR_MESSAGE,
                request,
                details
        );
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceAccess(
            ResourceAccessException ex, HttpServletRequest request) {
        log.error("CKYC connectivity error at path={}", request.getRequestURI(), ex);
        return build(
                HttpStatus.GATEWAY_TIMEOUT,
                ApplicationConstant.Error.CKYC_CONNECTIVITY_ERROR_CODE,
                ApplicationConstant.Error.CKYC_CONNECTIVITY_ERROR_MESSAGE,
                request,
                ex.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(
                HttpStatus.BAD_REQUEST,
                ApplicationConstant.Error.INVALID_PARAMETER_CODE,
                ApplicationConstant.Error.INVALID_PARAMETER_MESSAGE,
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnhandledException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at path={}", request.getRequestURI(), ex);
        return build(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApplicationConstant.Error.UNEXPECTED_ERROR_CODE,
                ApplicationConstant.Error.UNEXPECTED_ERROR_MESSAGE,
                request,
                ex.getMessage()
        );
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String errorCode,
            String message,
            HttpServletRequest request,
            Object details
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                status.name(),
                status.value(),
                errorCode,
                maskingUtil.maskSensitive(message),
                request.getRequestURI(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                details
        );
        return ResponseEntity.status(status).body(body);
    }

    private String defaultMessage(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
