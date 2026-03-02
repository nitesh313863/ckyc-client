package com.example.ckyc.serviceImpl;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.exception.CkycValidationException;
import com.example.ckyc.model.ApiResponse;
import com.example.ckyc.util.FieldValidationUtil;
import com.example.ckyc.util.ImageValidationUtil;
import com.example.ckyc.util.RequestIdGenerator;
import com.example.ckyc.service.UploadService;
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

    @Override
    public ApiResponse<Map<String, Object>> upload(CkycUploadRequest request) {
        if (!ckycProperties.isAllowNonStandardUploadUpdateApi()) {
            throw new CkycValidationException(
                    "CKYC upload via API is not enabled. As per CKYC public docs, upload/update are SFTP-driven flows."
            );
        }
        fieldValidationUtil.validateUploadRequest(request);
        imageValidationUtil.validateUploadRequest(request);
        String requestId = requestIdGenerator.nextRequestId();
        log.info("Preparing CKYC upload request requestId={}", requestId);

        String pidData = xmlBuilderService.buildUploadPidData(request, LocalDateTime.now().format(REQUEST_TS_FORMAT));
        String signedRequest = ckycEnvelopeService.encryptAndSign(OPERATION_UPLOAD, requestId, pidData);
        String rawResponse = ckycApiClient.postXml(ckycProperties.getUploadUrl(), signedRequest, requestId, OPERATION_UPLOAD);

        Map<String, Object> processed = ckycResponseService.processResponse(rawResponse, requestId, OPERATION_UPLOAD);
        String error = (String) processed.get("error");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestXml", signedRequest);
        data.put("rawResponseXml", rawResponse);
        data.putAll(processed);

        if (error != null && !error.isBlank()) {
            if (isDuplicate(error)) {
                auditService.record(OPERATION_UPLOAD, requestId, "DUPLICATE", error);
                return ApiResponse.of(requestId, "DUPLICATE", "CKYC record already exists", data);
            }
            throw new CkycValidationException(error);
        }

        auditService.record(OPERATION_UPLOAD, requestId, "SUCCESS", "upload completed");
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
}
