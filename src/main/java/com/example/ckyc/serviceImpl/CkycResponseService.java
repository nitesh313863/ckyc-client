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
            PublicKey cersaiPublicKey = KeyLoaderUtil.loadPublicKeyFromCer(ckycProperties.getCersaiCert());
            boolean signatureValid = XmlUtil.verifyXmlSignature(responseDoc, cersaiPublicKey);
            log.info("CKYC response signature validation operation={} requestId={} result={}", operation, requestId, signatureValid);
            auditService.record(operation, requestId, "SIGNATURE_VALIDATION", "valid=" + signatureValid);
            if (!signatureValid) {
                throw new CkycSignatureException("Response signature verification failed");
            }

            String error = XmlHelper.getTagValue(responseDoc, "ERROR");
            String errorCode = XmlHelper.getTagValue(responseDoc, "ERROR_CODE");
            String encryptedSessionKey = XmlHelper.getTagValue(responseDoc, "SESSION_KEY");
            String encryptedPid = XmlHelper.getTagValue(responseDoc, "PID");

            String decryptedPidData = null;
            if (encryptedSessionKey != null && !encryptedSessionKey.isBlank()
                    && encryptedPid != null && !encryptedPid.isBlank()) {
                try {
                    PrivateKey privateKey = KeyLoaderUtil.loadPrivateKeyFromPKCS12(
                            ckycProperties.getP12Path(),
                            ckycProperties.getP12Password(),
                            ckycProperties.getP12Alias()
                    );
                    byte[] sessionKey = CryptoUtil.decryptRSA(encryptedSessionKey, privateKey);
                    decryptedPidData = CryptoUtil.decryptAES(encryptedPid, sessionKey);
                } catch (Exception ex) {
                    throw new CkycEncryptionException("Failed to decrypt CKYC response", ex);
                }
            }
            log.info(
                    "CKYC response processed successfully operation={} requestId={} errorPresent={}",
                    operation,
                    requestId,
                    error != null && !error.isBlank()
            );

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("signatureValid", signatureValid);
            output.put("errorCode", (errorCode == null || errorCode.isBlank()) ? null : errorCode);
            output.put("error", (error == null || error.isBlank()) ? null : error);
            output.put("decryptedPidData", decryptedPidData);
            return output;
        } catch (CkycException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Unexpected error while processing CKYC response", e);
            throw new CkycException("Failed to process CKYC response", e);
        }
    }
}
