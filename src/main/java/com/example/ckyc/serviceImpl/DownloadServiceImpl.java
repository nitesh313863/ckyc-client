package com.example.ckyc.serviceImpl;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.exception.CkycValidationException;
import com.example.ckyc.model.ApiResponse;
import com.example.ckyc.util.FieldValidationUtil;
import com.example.ckyc.util.MaskingUtil;
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
    private final FieldValidationUtil fieldValidationUtil;
    private final MaskingUtil maskingUtil;

    @Override
    public ApiResponse<Map<String, Object>> download(CkycDownloadRequest request) {
        long startNanos = System.nanoTime();
        fieldValidationUtil.validateDownloadRequest(request);
        String requestId = requestIdGenerator.nextRequestId();
        log.info("CKYC download started requestId={} ckycNo={}", requestId, maskingUtil.maskSensitive(request.getCkycNo()));

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
            log.info(
                    "CKYC download otp-triggered requestId={} elapsedMs={} message={}",
                    requestId,
                    elapsedMs(startNanos),
                    maskingUtil.maskSensitive(error)
            );
            return ApiResponse.of(requestId, "OTP_SENT", "OTP sent to registered contact", data);
        }
        if (error != null && !error.isBlank()) {
            log.error(
                    "CKYC download failed requestId={} errorCode={} elapsedMs={} error={}",
                    requestId,
                    processed.get("errorCode"),
                    elapsedMs(startNanos),
                    maskingUtil.maskSensitive(error)
            );
            throw new CkycValidationException(error);
        }

        auditService.record(OPERATION_DOWNLOAD, requestId, "SUCCESS", "record returned");
        log.info(
                "CKYC download completed requestId={} status={} ckycNo={} referenceId={} elapsedMs={}",
                requestId,
                processed.get("status"),
                maskingUtil.maskSensitive(value(processed.get("ckycNo"))),
                value(processed.get("ckycReferenceId")),
                elapsedMs(startNanos)
        );
        return ApiResponse.of(requestId, "SUCCESS", "CKYC record fetched successfully", data);
    }

    @Override
    public ApiResponse<Map<String, Object>> validateOtp(CkycValidateOtpRequest request) {
        long startNanos = System.nanoTime();
        String requestId = requestIdGenerator.nextRequestId();
        log.info("CKYC validate-otp started requestId={} ckycNo={}", requestId, maskingUtil.maskSensitive(request.getCkycNo()));

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
            log.error(
                    "CKYC validate-otp failed requestId={} errorCode={} elapsedMs={} error={}",
                    requestId,
                    processed.get("errorCode"),
                    elapsedMs(startNanos),
                    maskingUtil.maskSensitive(error)
            );
            throw new CkycValidationException(error);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestXml", signedRequest);
        data.put("rawResponseXml", rawResponse);
        data.putAll(processed);

        auditService.record(OPERATION_VALIDATE_OTP, requestId, "SUCCESS", "otp validated");
        log.info(
                "CKYC validate-otp completed requestId={} status={} ckycNo={} referenceId={} elapsedMs={}",
                requestId,
                processed.get("status"),
                maskingUtil.maskSensitive(value(processed.get("ckycNo"))),
                value(processed.get("ckycReferenceId")),
                elapsedMs(startNanos)
        );
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

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private String value(Object input) {
        return input == null ? null : input.toString();
    }
}
