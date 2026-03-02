package com.example.ckyc.serviceImpl;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.exception.CkycValidationException;
import com.example.ckyc.model.ApiResponse;
import com.example.ckyc.util.RequestIdGenerator;
import com.example.ckyc.service.DownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DownloadServiceImpl implements DownloadService {

    private static final DateTimeFormatter REQUEST_TS_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final String ENVELOPE_OPERATION = "CKYC_INQ";
    private static final String OPERATION_DOWNLOAD = "CKYC_DOWNLOAD";
    private static final String OPERATION_VALIDATE_OTP = "CKYC_VALIDATE_OTP";

    private final CkycProperties ckycProperties;
    private final XmlBuilderService xmlBuilderService;
    private final CkycEnvelopeService ckycEnvelopeService;
    private final CkycApiClient ckycApiClient;
    private final CkycResponseService ckycResponseService;
    private final RequestIdGenerator requestIdGenerator;
    private final AuditService auditService;

    @Override
    public ApiResponse<Map<String, Object>> download(CkycDownloadRequest request) {
        String requestId = requestIdGenerator.nextRequestId();
        log.info("Preparing CKYC download request requestId={}", requestId);

        String pidData = xmlBuilderService.buildDownloadPidData(request, LocalDateTime.now().format(REQUEST_TS_FORMAT));
        String signedRequest = ckycEnvelopeService.encryptAndSign(ENVELOPE_OPERATION, requestId, pidData);
        String rawResponse = ckycApiClient.postXml(ckycProperties.getDownloadUrl(), signedRequest, requestId, OPERATION_DOWNLOAD);

        Map<String, Object> processed = ckycResponseService.processResponse(rawResponse, requestId, OPERATION_DOWNLOAD);
        String error = normalize((String) processed.get("error"));
        boolean otpTriggered = isOtpTriggered(error + " " + rawResponse);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestXml", signedRequest);
        data.put("rawResponseXml", rawResponse);
        data.putAll(processed);

        if (otpTriggered) {
            auditService.record(OPERATION_DOWNLOAD, requestId, "OTP_SENT", error);
            return ApiResponse.of(requestId, "OTP_SENT", "OTP sent to registered contact", data);
        }
        if (error != null && !error.isBlank()) {
            throw new CkycValidationException(error);
        }

        auditService.record(OPERATION_DOWNLOAD, requestId, "SUCCESS", "record returned");
        return ApiResponse.of(requestId, "SUCCESS", "CKYC record fetched successfully", data);
    }

    @Override
    public ApiResponse<Map<String, Object>> validateOtp(CkycValidateOtpRequest request) {
        String requestId = requestIdGenerator.nextRequestId();
        log.info("Preparing CKYC validate OTP request requestId={}", requestId);

        String pidData = xmlBuilderService.buildValidateOtpPidData(request, LocalDateTime.now().format(REQUEST_TS_FORMAT));
        String signedRequest = ckycEnvelopeService.encryptAndSign(ENVELOPE_OPERATION, requestId, pidData);
        String rawResponse = ckycApiClient.postXml(
                ckycProperties.getValidateOtpUrl(),
                signedRequest,
                requestId,
                OPERATION_VALIDATE_OTP
        );

        Map<String, Object> processed = ckycResponseService.processResponse(rawResponse, requestId, OPERATION_VALIDATE_OTP);
        String error = normalize((String) processed.get("error"));
        if (error != null && !error.isBlank()) {
            throw new CkycValidationException(error);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestXml", signedRequest);
        data.put("rawResponseXml", rawResponse);
        data.putAll(processed);

        auditService.record(OPERATION_VALIDATE_OTP, requestId, "SUCCESS", "otp validated");
        return ApiResponse.of(requestId, "SUCCESS", "OTP validated and CKYC record fetched successfully", data);
    }

    private boolean isOtpTriggered(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String normalized = content.toUpperCase(Locale.ROOT);
        return ckycProperties.getDownload().getOtpTriggerKeywords()
                .stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(keyword -> keyword.toUpperCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
