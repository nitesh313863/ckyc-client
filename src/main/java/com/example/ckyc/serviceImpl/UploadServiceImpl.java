package com.example.ckyc.serviceImpl;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.dto.CkycApiResponsePayload;
import com.example.ckyc.dto.CkycResponseDto;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.exception.CkycValidationException;
import com.example.ckyc.model.ApiResponse;
import com.example.ckyc.util.FieldValidationUtil;
import com.example.ckyc.util.ImageValidationUtil;
import com.example.ckyc.util.MaskingUtil;
import com.example.ckyc.util.RequestIdGenerator;
import com.example.ckyc.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final DateTimeFormatter REQUEST_TS_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final String OPERATION_UPLOAD = "CKYC_UPLOAD";

    private final CkycProperties ckycProperties;
    private final XmlBuilderService xmlBuilderService;
    private final CkycEnvelopeService ckycEnvelopeService;
    private final CkycApiClient ckycApiClient;
    private final CkycResponseService ckycResponseService;
    private final RequestIdGenerator requestIdGenerator;
    private final AuditService auditService;
    private final ImageValidationUtil imageValidationUtil;
    private final FieldValidationUtil fieldValidationUtil;
    private final MaskingUtil maskingUtil;

    @Override
    public ApiResponse<CkycApiResponsePayload> upload(CkycUploadRequest request) {
        long startNanos = System.nanoTime();
        if (!ckycProperties.isAllowNonStandardUploadUpdateApi()) {
            throw new CkycValidationException(
                    "CKYC upload via API is not enabled. As per CKYC public docs, upload/update are SFTP-driven flows."
            );
        }
        log.warn("CKYC upload is running in non-standard direct API mode (official channel is SFTP)");
        fieldValidationUtil.validateUploadRequest(request);
        imageValidationUtil.validateUploadRequest(request);
        String requestId = requestIdGenerator.nextRequestId();
        int identityCount = request.getIdentityDetails() == null ? 0 : request.getIdentityDetails().size();
        int addressCount = request.getAddressDetails() == null ? 0 : request.getAddressDetails().size();
        int imageCount = request.getImageDetails() == null ? 0 : request.getImageDetails().size();
        log.info(
                "CKYC upload started requestId={} identityCount={} addressCount={} imageCount={}",
                requestId,
                identityCount,
                addressCount,
                imageCount
        );

        String pidData = xmlBuilderService.buildUploadPidData(request, LocalDateTime.now().format(REQUEST_TS_FORMAT));
        String signedRequest = ckycEnvelopeService.encryptAndSign(OPERATION_UPLOAD, requestId, pidData);
        String rawResponse = ckycApiClient.postXml(ckycProperties.getUploadUrl(), signedRequest, requestId, OPERATION_UPLOAD);

        CkycResponseDto processed = ckycResponseService.processResponse(rawResponse, requestId, OPERATION_UPLOAD);
        String error = processed.getError();

        CkycApiResponsePayload data = CkycApiResponsePayload.builder()
                .requestXml(signedRequest)
                .rawResponseXml(rawResponse)
                .response(processed)
                .build();

        if (error != null && !error.isBlank()) {
            if (isDuplicate(error)) {
                auditService.record(OPERATION_UPLOAD, requestId, "DUPLICATE", error);
                log.warn(
                        "CKYC upload duplicate requestId={} errorCode={} elapsedMs={} error={}",
                        requestId,
                        processed.getErrorCode(),
                        elapsedMs(startNanos),
                        maskingUtil.maskSensitive(error)
                );
                return ApiResponse.of(requestId, "DUPLICATE", "CKYC record already exists", data);
            }
            log.error(
                    "CKYC upload failed requestId={} errorCode={} elapsedMs={} error={}",
                    requestId,
                    processed.getErrorCode(),
                    elapsedMs(startNanos),
                    maskingUtil.maskSensitive(error)
            );
            throw new CkycValidationException(error);
        }

        auditService.record(OPERATION_UPLOAD, requestId, "SUCCESS", "upload completed");
        log.info(
                "CKYC upload completed requestId={} status={} ckycNo={} referenceId={} elapsedMs={}",
                requestId,
                processed.getStatus(),
                maskingUtil.maskSensitive(processed.getCkycNo()),
                processed.getCkycReferenceId(),
                elapsedMs(startNanos)
        );
        return ApiResponse.of(requestId, "SUCCESS", "CKYC upload completed successfully", data);
    }

    private boolean isDuplicate(String errorMessage) {
        String normalized = errorMessage.toUpperCase(Locale.ROOT);
        return ckycProperties.getUpload().getDuplicateKeywords()
                .stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(keyword -> keyword.toUpperCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

}
