package com.example.ckyc.serviceImpl;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.exception.CkycEncryptionException;
import com.example.ckyc.exception.CkycException;
import com.example.ckyc.exception.CkycSignatureException;
import com.example.ckyc.util.CryptoUtil;
import com.example.ckyc.util.KeyLoaderUtil;
import com.example.ckyc.util.XmlHelper;
import com.example.ckyc.util.XmlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class CkycResponseService {

    private final CkycProperties ckycProperties;
    private final AuditService auditService;

    public Map<String, Object> processResponse(String xmlResponse) {
        return processResponse(xmlResponse, null, "SEARCH");
    }

    public Map<String, Object> processResponse(String xmlResponse, String requestId, String operation) {
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

            String decryptedPidData = null;
            if (encryptedSessionKey != null && !encryptedSessionKey.isBlank()
                    && encryptedPid != null && !encryptedPid.isBlank()) {
                try {
                    PrivateKey privateKey = loadProjectPrivateKey();
                    byte[] sessionKey = CryptoUtil.decryptRSA(encryptedSessionKey, privateKey);
                    decryptedPidData = CryptoUtil.decryptAES(encryptedPid, sessionKey);
                } catch (Exception ex) {
                    throw new CkycEncryptionException("Failed to decrypt CKYC response", ex);
                }
            }
            Map<String, String> pidFields = parsePidFields(decryptedPidData);
            ckycNo = firstNonBlank(ckycNo, pidFields.get("ckycNo"));
            ckycReferenceId = firstNonBlank(ckycReferenceId, pidFields.get("ckycReferenceId"));
            status = firstNonBlank(status, pidFields.get("status"));
            errorCode = firstNonBlank(errorCode, pidFields.get("errorCode"));
            errorDesc = firstNonBlank(errorDesc, pidFields.get("errorDesc"));

            log.info(
                    "CKYC response processed successfully operation={} requestId={} errorPresent={}",
                    operation,
                    requestId,
                    errorDesc != null && !errorDesc.isBlank()
            );

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("signatureValid", signatureValid);
            output.put("status", normalize(status));
            output.put("ckycNo", normalize(ckycNo));
            output.put("ckycReferenceId", normalize(ckycReferenceId));
            output.put("errorCode", normalize(errorCode));
            output.put("errorDesc", normalize(errorDesc));
            output.put("error", normalize(errorDesc));
            output.put("decryptedPidData", decryptedPidData);
            return output;
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

    private Map<String, String> parsePidFields(String decryptedPidData) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (decryptedPidData == null || decryptedPidData.isBlank()) {
            return fields;
        }
        try {
            String wrapped = "<ROOT>" + decryptedPidData + "</ROOT>";
            Document doc = XmlHelper.parseFragment(wrapped);
            fields.put("ckycNo", firstNonBlank(
                    getTagValue(doc, "CKYC_NO"),
                    getTagValue(doc, "CKYCNUMBER")
            ));
            fields.put("ckycReferenceId", firstNonBlank(
                    getTagValue(doc, "CKYC_REFERENCE_ID"),
                    getTagValue(doc, "REFERENCE_ID"),
                    getTagValue(doc, "REF_ID")
            ));
            fields.put("status", firstNonBlank(
                    getTagValue(doc, "STATUS"),
                    getTagValue(doc, "STATUS_CODE")
            ));
            fields.put("errorCode", firstNonBlank(
                    getTagValue(doc, "ERRORCODE"),
                    getTagValue(doc, "ERROR_CODE"),
                    getTagValue(doc, "ERRCODE")
            ));
            fields.put("errorDesc", firstNonBlank(
                    getTagValue(doc, "ERRORDESC"),
                    getTagValue(doc, "ERROR_DESC"),
                    getTagValue(doc, "ERROR"),
                    getTagValue(doc, "MESSAGE")
            ));
            fields.entrySet().removeIf(entry -> Objects.isNull(entry.getValue()) || entry.getValue().isBlank());
            return fields;
        } catch (Exception ex) {
            log.warn("Unable to parse decrypted PID fields from CKYC response");
            return Map.of();
        }
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
