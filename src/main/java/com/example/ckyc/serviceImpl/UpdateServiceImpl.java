package com.example.ckyc.serviceImpl;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.dto.CkycResponseDto;
import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.dto.CkycUpdateResponseDto;
import com.example.ckyc.exception.CkycEncryptionException;
import com.example.ckyc.exception.CkycSignatureException;
import com.example.ckyc.exception.CkycUpstreamException;
import com.example.ckyc.exception.CkycValidationException;
import com.example.ckyc.util.CkycEncrypter;
import com.example.ckyc.util.DigitalSigner;
import com.example.ckyc.util.ImageMapperUtil;
import com.example.ckyc.util.ImageValidationUtil;
import com.example.ckyc.util.MaskingUtil;
import com.example.ckyc.util.RequestIdGenerator;
import com.example.ckyc.util.FieldValidationUtil;
import com.example.ckyc.service.UpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class UpdateServiceImpl implements UpdateService {

    private static final String OPERATION_UPDATE = "CKYC_UPDATE";
    private static final DateTimeFormatter REQUEST_TS_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private final CkycProperties ckycProperties;
    private final UpdateXmlBuilder updateXmlBuilder;
    private final CkycApiClient ckycApiClient;
    private final CkycResponseService ckycResponseService;
    private final RequestIdGenerator requestIdGenerator;
    private final AuditService auditService;
    private final ImageMapperUtil imageMapperUtil;
    private final ImageValidationUtil imageValidationUtil;
    private final MaskingUtil maskingUtil;
    private final FieldValidationUtil fieldValidationUtil;

    @Override
    public CkycUpdateResponseDto update(CkycUpdateRequestDto request) {
        long startNanos = System.nanoTime();
        validateRequest(request);
        if (!ckycProperties.isAllowNonStandardUploadUpdateApi()) {
            throw new CkycValidationException(
                    "CKYC update via API is not enabled. As per CKYC public docs, upload/update are SFTP-driven flows."
            );
        }
        log.warn("CKYC update is running in non-standard direct API mode (official channel is SFTP)");
        if (ckycProperties.getUpdateUrl() == null || ckycProperties.getUpdateUrl().isBlank()) {
            throw new CkycValidationException("CKYC update endpoint is not configured");
        }
        String requestId = requestIdGenerator.nextRequestId();
        String timestamp = LocalDateTime.now().format(REQUEST_TS_FORMAT);

        List<ImageMapperUtil.NormalizedImage> normalizedImages = normalizeImages(request.getImageList());
        imageValidationUtil.validateUpdateImages(normalizedImages);
        String pidData = updateXmlBuilder.buildPidData(request, timestamp, normalizedImages);
        log.info("Prepared CKYC update PID_DATA requestId={} payload={}", requestId, maskingUtil.maskSensitive(pidData));

        String signedXml = buildSignedRequestXml(requestId, pidData);
        String rawResponse = ckycApiClient.postXml(ckycProperties.getUpdateUrl(), signedXml, requestId, OPERATION_UPDATE);

        CkycResponseDto processed = ckycResponseService.processResponse(rawResponse, requestId, OPERATION_UPDATE);
        String error = firstNonBlank(processed.getErrorDesc(), processed.getError());
        String errorCode = processed.getErrorCode();
        String responseStatus = processed.getStatus();
        String responseCkycNo = firstNonBlank(processed.getCkycNo(), request.getCkycNo());

        if (isDuplicate(error, errorCode)) {
            auditService.record(OPERATION_UPDATE, requestId, "DUPLICATE", error);
            log.warn(
                    "CKYC update duplicate requestId={} ckycNo={} errorCode={} elapsedMs={} error={}",
                    requestId,
                    maskingUtil.maskSensitive(responseCkycNo),
                    errorCode,
                    elapsedMs(startNanos),
                    maskingUtil.maskSensitive(error)
            );
            return CkycUpdateResponseDto.builder()
                    .status("DUPLICATE")
                    .message("Duplicate update attempt")
                    .ckycNo(responseCkycNo)
                    .updateType(request.getUpdateType().toUpperCase(Locale.ROOT))
                    .errorCode(errorCode == null || errorCode.isBlank() ? "DUPLICATE_UPDATE" : errorCode)
                    .build();
        }

        if (!isSuccessStatus(responseStatus)) {
            String upstreamMessage = (error == null || error.isBlank())
                    ? "CKYC update rejected with status: " + responseStatus
                    : error;
            log.error(
                    "CKYC update failed requestId={} ckycNo={} status={} errorCode={} elapsedMs={} error={}",
                    requestId,
                    maskingUtil.maskSensitive(responseCkycNo),
                responseStatus,
                errorCode,
                elapsedMs(startNanos),
                maskingUtil.maskSensitive(upstreamMessage)
            );
            throw new CkycUpstreamException(upstreamMessage, 502, false, rawResponse);
        }

        auditService.record(OPERATION_UPDATE, requestId, "SUCCESS", "update completed");
        log.info(
                "CKYC update completed requestId={} ckycNo={} status={} errorCode={} elapsedMs={}",
                requestId,
                maskingUtil.maskSensitive(responseCkycNo),
                responseStatus,
                errorCode,
                elapsedMs(startNanos)
        );
        return CkycUpdateResponseDto.builder()
                .status("SUCCESS")
                .message("CKYC update successful")
                .ckycNo(responseCkycNo)
                .updateType(request.getUpdateType().toUpperCase(Locale.ROOT))
                .errorCode(errorCode)
                .build();
    }

    private String buildSignedRequestXml(String requestId, String pidData) {
        String unsignedXml;
        try {
            CkycEncrypter encrypter = new CkycEncrypter(resolveCkycPublicCertPath());
            byte[] sessionKey = encrypter.generateSessionKey();
            byte[] encryptedPidBytes = encrypter.encryptUsingSessionKey(
                    sessionKey,
                    pidData.getBytes(StandardCharsets.UTF_8)
            );
            String encryptedPid = Base64.encodeBase64String(encryptedPidBytes);

            byte[] encryptedSessionKeyBytes = encrypter.encryptUsingPublicKey(
                    sessionKey,
                    ckycProperties.getVersion()
            );
            String encryptedSessionKey = Base64.encodeBase64String(encryptedSessionKeyBytes);

            unsignedXml = updateXmlBuilder.buildUnsignedRequest(
                    ckycProperties.getFiCode(),
                    requestId,
                    ckycProperties.getVersion(),
                    encryptedSessionKey,
                    encryptedPid
            );
        } catch (Exception ex) {
            throw new CkycEncryptionException("Failed to encrypt CKYC update request", ex);
        }

        try {
            DigitalSigner signer = buildSigner();
            return signer.signXML(unsignedXml, true, ckycProperties.getVersion());
        } catch (Exception ex) {
            throw new CkycSignatureException("Failed to sign CKYC update request", ex);
        }
    }

    private List<ImageMapperUtil.NormalizedImage> normalizeImages(List<CkycUpdateRequestDto.ImageDetails> imageList) {
        List<ImageMapperUtil.NormalizedImage> normalized = new ArrayList<>();
        if (imageList == null || imageList.isEmpty()) {
            return normalized;
        }
        for (CkycUpdateRequestDto.ImageDetails image : imageList) {
            normalized.add(imageMapperUtil.normalize(image));
        }
        return normalized;
    }

    private void validateRequest(CkycUpdateRequestDto request) {
        fieldValidationUtil.validateUpdateRequest(request);
        if (request.getUpdateType() == null || request.getUpdateType().isBlank()) {
            throw new CkycValidationException("updateType is mandatory");
        }
        String updateType = request.getUpdateType().trim().toUpperCase(Locale.ROOT);
        if (!"FULL".equals(updateType) && !"PARTIAL".equals(updateType)) {
            throw new CkycValidationException("updateType must be FULL or PARTIAL");
        }
        if (!hasAnyUpdateSection(request)) {
            throw new CkycValidationException("At least one update section must be provided");
        }
    }

    private boolean hasAnyUpdateSection(CkycUpdateRequestDto request) {
        return request.getPersonalDetails() != null
                || request.getAddressDetails() != null
                || (request.getIdentityList() != null && !request.getIdentityList().isEmpty())
                || (request.getImageList() != null && !request.getImageList().isEmpty());
    }

    private boolean isDuplicate(String error, String errorCode) {
        String msg = error == null ? "" : error.toUpperCase(Locale.ROOT);
        String code = errorCode == null ? "" : errorCode.toUpperCase(Locale.ROOT);
        return msg.contains("DUPLICATE") || msg.contains("ALREADY EXISTS") || code.contains("DUPLICATE");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private boolean isSuccessStatus(String status) {
        if (status == null || status.isBlank()) {
            return true;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "SUCCESS".equals(normalized) || "S".equals(normalized) || "0".equals(normalized);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private String resolveCkycPublicCertPath() {
        return hasText(ckycProperties.getCkycPublicKeyPath())
                ? ckycProperties.getCkycPublicKeyPath()
                : ckycProperties.getCersaiCert();
    }

    private DigitalSigner buildSigner() {
        if (!hasText(ckycProperties.getP12Path()) || !hasText(ckycProperties.getP12Password())) {
            throw new CkycSignatureException("PKCS12 path/password not configured for CKYC signing");
        }
        if (hasText(ckycProperties.getP12Alias())) {
            return new DigitalSigner(
                    ckycProperties.getP12Path(),
                    ckycProperties.getP12Password().toCharArray(),
                    ckycProperties.getP12Alias()
            );
        }
        return new DigitalSigner(
                ckycProperties.getP12Path(),
                ckycProperties.getP12Password().toCharArray()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
