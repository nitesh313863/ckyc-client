package com.example.ckyc.serviceImpl;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.exception.CkycEncryptionException;
import com.example.ckyc.exception.CkycException;
import com.example.ckyc.exception.CkycSignatureException;
import com.example.ckyc.dto.CkycDownloadResponseDto;
import com.example.ckyc.dto.CkycResponseDto;
import com.example.ckyc.dto.CkycSearchResponseDto;
import com.example.ckyc.util.CkycEncrypter;
import com.example.ckyc.util.CkycPidDataParser;
import com.example.ckyc.util.CkycResponseMapper;
import com.example.ckyc.util.KeyLoaderUtil;
import com.example.ckyc.util.XmlHelper;
import com.example.ckyc.util.XmlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class CkycResponseService {

    private final CkycProperties ckycProperties;
    private final AuditService auditService;

    public CkycResponseDto processResponse(String xmlResponse) {
        return processResponse(xmlResponse, null, "SEARCH");
    }

    public CkycResponseDto processResponse(String xmlResponse, String requestId, String operation) {
        try {
            log.info("Processing CKYC response operation={} requestId={}", operation, requestId);
            Document responseDoc = XmlHelper.parse(xmlResponse);
            PublicKey cersaiPublicKey = KeyLoaderUtil.loadPublicKeyFromCer(resolveCkycPublicCertPath());
            boolean signatureValid = XmlUtil.verifyXmlSignature(responseDoc, cersaiPublicKey);
            log.info("CKYC response signature validation operation={} requestId={} result={}", operation, requestId, signatureValid);
            auditService.record(operation, requestId, "SIGNATURE_VALIDATION", "valid=" + signatureValid);
            if (!signatureValid) {
                throw new CkycSignatureException("Response signature verification failed");
            }

            String status = firstNonBlank(
                    getTagValue(responseDoc, "STATUS"),
                    getTagValue(responseDoc, "STATUS_CODE")
            );
            String errorCode = firstNonBlank(
                    getTagValue(responseDoc, "ERRORCODE"),
                    getTagValue(responseDoc, "ERROR_CODE"),
                    getTagValue(responseDoc, "ERRCODE")
            );
            String errorDesc = firstNonBlank(
                    getTagValue(responseDoc, "ERRORDESC"),
                    getTagValue(responseDoc, "ERROR_DESC"),
                    getTagValue(responseDoc, "ERROR"),
                    getTagValue(responseDoc, "MESSAGE")
            );
            String responseFiCode = getTagValue(responseDoc, "FI_CODE");
            String responseRequestId = getTagValue(responseDoc, "REQUEST_ID");
            String responseReqDate = getTagValue(responseDoc, "REQ_DATE");
            String responseVersion = getTagValue(responseDoc, "VERSION");
            String ckycNo = firstNonBlank(
                    getTagValue(responseDoc, "CKYC_NO"),
                    getTagValue(responseDoc, "CKYCNUMBER")
            );
            String ckycReferenceId = firstNonBlank(
                    getTagValue(responseDoc, "CKYC_REFERENCE_ID"),
                    getTagValue(responseDoc, "REFERENCE_ID"),
                    getTagValue(responseDoc, "REF_ID")
            );
            String encryptedSessionKey = XmlHelper.getTagValue(responseDoc, "SESSION_KEY");
            String encryptedPid = XmlHelper.getTagValue(responseDoc, "PID");
            String responseError = firstNonBlank(
                    getTagValue(responseDoc, "ERROR"),
                    getTagValue(responseDoc, "ERROR_DESC"),
                    getTagValue(responseDoc, "ERRORDESC"),
                    getTagValue(responseDoc, "MESSAGE")
            );

            String decryptedPidData = null;
            CkycPidDataParser.ParsedPidData parsedPid = null;
            if (encryptedSessionKey != null && !encryptedSessionKey.isBlank()
                    && encryptedPid != null && !encryptedPid.isBlank()) {
                try {
                    CkycEncrypter encrypter = new CkycEncrypter();
                    byte[] encryptedSessionKeyBytes = Base64.decodeBase64(encryptedSessionKey);
                    byte[] encryptedPidBytes = Base64.decodeBase64(encryptedPid);

                    byte[] sessionKey;
                    if (hasText(ckycProperties.getProjectPrivateKeyPath())) {
                        PrivateKey privateKey = loadProjectPrivateKey();
                        sessionKey = encrypter.decryptUsingPrivateKey(
                                privateKey,
                                encryptedSessionKeyBytes,
                                ckycProperties.getVersion()
                        );
                    } else {
                        sessionKey = encrypter.decryptUsingPrivateKey(
                                ckycProperties.getP12Path(),
                                encryptedSessionKeyBytes,
                                ckycProperties.getP12Password(),
                                ckycProperties.getVersion(),
                                ckycProperties.getP12Alias()
                        );
                    }

                    byte[] decryptedPidBytes = encrypter.decryptUsingSessionKey(sessionKey, encryptedPidBytes);
                    decryptedPidData = new String(decryptedPidBytes, StandardCharsets.UTF_8);
                    parsedPid = CkycPidDataParser.parse(decryptedPidData);
                } catch (Exception ex) {
                    throw new CkycEncryptionException("Failed to decrypt CKYC response", ex);
                }
            }
            Map<String, String> pidFields = parsedPid == null ? new LinkedHashMap<>() : parsedPid.getFlat();
            ckycNo = firstNonBlank(ckycNo, pidFields.get("CKYC_NO"), pidFields.get("CKYCNUMBER"));
            ckycReferenceId = firstNonBlank(ckycReferenceId, pidFields.get("CKYC_REFERENCE_ID"), pidFields.get("REFERENCE_ID"), pidFields.get("REF_ID"));
            status = firstNonBlank(status, pidFields.get("STATUS"), pidFields.get("STATUS_CODE"));
            errorCode = firstNonBlank(errorCode, pidFields.get("ERRORCODE"), pidFields.get("ERROR_CODE"), pidFields.get("ERRCODE"));
            errorDesc = firstNonBlank(errorDesc, pidFields.get("ERRORDESC"), pidFields.get("ERROR_DESC"), pidFields.get("ERROR"), pidFields.get("MESSAGE"));
            errorDesc = firstNonBlank(errorDesc, responseError);

            log.info(
                    "CKYC response processed successfully operation={} requestId={} errorPresent={}",
                    operation,
                    requestId,
                    errorDesc != null && !errorDesc.isBlank()
            );

            String normalizedOperation = operation == null ? "" : operation.trim().toUpperCase(Locale.ROOT);
            CkycSearchResponseDto searchResponse = isSearchOperation(normalizedOperation)
                    ? CkycResponseMapper.toSearchResponse(parsedPid)
                    : null;
            CkycDownloadResponseDto downloadResponse = isDownloadOperation(normalizedOperation)
                    ? CkycResponseMapper.toDownloadResponse(parsedPid)
                    : null;

            return CkycResponseDto.builder()
                    .operation(operation)
                    .rootTag(responseDoc.getDocumentElement().getTagName())
                    .signatureValid(signatureValid)
                    .fiCode(normalize(responseFiCode))
                    .requestId(normalize(responseRequestId))
                    .reqDate(normalize(responseReqDate))
                    .version(normalize(responseVersion))
                    .status(normalize(status))
                    .ckycNo(normalize(ckycNo))
                    .ckycReferenceId(normalize(ckycReferenceId))
                    .errorCode(normalize(errorCode))
                    .errorDesc(normalize(errorDesc))
                    .error(normalize(errorDesc))
                    .decryptedPidData(decryptedPidData)
                    .otpRequired(isOtpTrigger(operation, errorDesc))
                    .pidDetails(parsedPid == null ? null : parsedPid.getStructured())
                    .pidFlat(parsedPid == null ? null : parsedPid.getFlat())
                    .searchResponse(searchResponse)
                    .downloadResponse(downloadResponse)
                    .build();
        } catch (CkycException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Unexpected error while processing CKYC response", e);
            throw new CkycException("Failed to process CKYC response", e);
        }
    }

    private String getTagValue(Document doc, String tagName) {
        return XmlHelper.getTagValue(doc, tagName);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isOtpTrigger(String operation, String errorDesc) {
        if (errorDesc == null || errorDesc.isBlank()) {
            return false;
        }
        if (operation == null) {
            return false;
        }
        String normalizedOp = operation.trim().toUpperCase(Locale.ROOT);
        if (!normalizedOp.contains("DOWNLOAD") && !normalizedOp.contains("OTP")) {
            return false;
        }
        Set<String> keywords = Set.copyOf(ckycProperties.getDownload().getOtpTriggerKeywords());
        String upper = errorDesc.toUpperCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (upper.contains(keyword.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return upper.contains("OTP");
    }

    private boolean isSearchOperation(String operation) {
        if (operation == null || operation.isBlank()) {
            return false;
        }
        return operation.contains("SEARCH") || operation.contains("INQ");
    }

    private boolean isDownloadOperation(String operation) {
        if (operation == null || operation.isBlank()) {
            return false;
        }
        return operation.contains("DOWNLOAD") || operation.contains("OTP");
    }

    private String resolveCkycPublicCertPath() {
        return hasText(ckycProperties.getCkycPublicKeyPath())
                ? ckycProperties.getCkycPublicKeyPath()
                : ckycProperties.getCersaiCert();
    }

    private PrivateKey loadProjectPrivateKey() throws Exception {
        if (hasText(ckycProperties.getProjectPrivateKeyPath())) {
            return KeyLoaderUtil.loadPrivateKeyFromPem(ckycProperties.getProjectPrivateKeyPath());
        }
        return KeyLoaderUtil.loadPrivateKeyFromPKCS12(
                ckycProperties.getP12Path(),
                ckycProperties.getP12Password(),
                ckycProperties.getP12Alias()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
