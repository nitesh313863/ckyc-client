package com.example.ckyc.service;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.exception.CkycEncryptionException;
import com.example.ckyc.exception.CkycSignatureException;
import com.example.ckyc.exception.CkycValidationException;
import com.example.ckyc.util.CryptoUtil;
import com.example.ckyc.util.KeyLoaderUtil;
import com.example.ckyc.util.RequestIdGenerator;
import com.example.ckyc.util.XmlHelper;
import com.example.ckyc.util.XmlUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class CkycService {

    private static final DateTimeFormatter REQUEST_TS_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final Map<String, Pattern> ID_TYPE_PATTERNS = new HashMap<>();
    private static final Map<String, Integer> ID_TYPE_MAX_LEN = new HashMap<>();

    static {
        ID_TYPE_PATTERNS.put("A", Pattern.compile("^[A-Za-z][0-9]{7}$"));
        ID_TYPE_PATTERNS.put("C", Pattern.compile("^[A-Z]{3}[ABCFGHJLPT][A-Z][0-9]{4}[A-Z]$"));
        ID_TYPE_PATTERNS.put("Z", Pattern.compile("^[0-9]{14}$"));
        ID_TYPE_PATTERNS.put("Y", Pattern.compile("^[A-Za-z0-9]{14}$"));
        ID_TYPE_PATTERNS.put("02", Pattern.compile("^.{1,60}$"));
        ID_TYPE_PATTERNS.put("03", Pattern.compile("^.{1,60}$"));

        ID_TYPE_MAX_LEN.put("A", 20);
        ID_TYPE_MAX_LEN.put("B", 20);
        ID_TYPE_MAX_LEN.put("C", 10);
        ID_TYPE_MAX_LEN.put("D", 20);
        ID_TYPE_MAX_LEN.put("E", 165);
        ID_TYPE_MAX_LEN.put("F", 40);
        ID_TYPE_MAX_LEN.put("G", 20);
        ID_TYPE_MAX_LEN.put("Z", 14);
        ID_TYPE_MAX_LEN.put("Y", 14);
        ID_TYPE_MAX_LEN.put("02", 60);
        ID_TYPE_MAX_LEN.put("03", 60);
    }

    private final CkycProperties ckycProperties;
    private final RequestIdGenerator requestIdGenerator;

    public String prepareSearchRequest(String idType, String idNo) {
        try {
            validateInput(idType, idNo);
            log.info("Preparing CKYC request for idType={}", idType);

            byte[] sessionKey = CryptoUtil.generateSessionKey();
            String requestId = requestIdGenerator.nextRequestId();
            String normalizedIdType = idType.trim().toUpperCase();
            String normalizedIdNo = idNo.trim();
            log.debug("Generated CKYC requestId={}", requestId);

            String pidData = String.format(
                    "<PID_DATA>" +
                            "<DATE_TIME>%s</DATE_TIME>" +
                            "<ID_TYPE>%s</ID_TYPE>" +
                            "<ID_NO>%s</ID_NO>" +
                            "</PID_DATA>",
                    LocalDateTime.now().format(REQUEST_TS_FORMAT),
                    xmlEscape(normalizedIdType),
                    xmlEscape(normalizedIdNo)
            );

            String encryptedPid = CryptoUtil.encryptAES(pidData, sessionKey);

            PublicKey cersaiKey = KeyLoaderUtil.loadPublicKeyFromCer(ckycProperties.getCersaiCert());
            String encryptedSessionKey = CryptoUtil.encryptRSA(sessionKey, cersaiKey);

            String xml = String.format(
                    "<REQ_ROOT>" +
                            "<HEADER>" +
                            "<FI_CODE>%s</FI_CODE>" +
                            "<REQUEST_ID>%s</REQUEST_ID>" +
                            "<VERSION>%s</VERSION>" +
                            "</HEADER>" +
                            "<CKYC_INQ>" +
                            "<PID>%s</PID>" +
                            "<SESSION_KEY>%s</SESSION_KEY>" +
                            "</CKYC_INQ>" +
                            "</REQ_ROOT>",
                    xmlEscape(ckycProperties.getFiCode()),
                    requestId,
                    xmlEscape(ckycProperties.getVersion()),
                    encryptedPid,
                    encryptedSessionKey
            );
            PrivateKey privateKey = KeyLoaderUtil.loadPrivateKeyFromPKCS12(
                    ckycProperties.getP12Path(),
                    ckycProperties.getP12Password(),
                    ckycProperties.getP12Alias()
            );
            X509Certificate cert = KeyLoaderUtil.loadCertificateFromPKCS12(
                    ckycProperties.getP12Path(),
                    ckycProperties.getP12Password(),
                    ckycProperties.getP12Alias()
            );

            Document doc = XmlHelper.parse(xml);
            XmlUtil.signXml(doc, privateKey, cert);

            log.info("CKYC request prepared successfully with requestId={}", requestId);
            return XmlHelper.toString(doc);
        } catch (CkycValidationException ex) {
            throw ex;
        } catch (CkycSignatureException ex) {
            throw ex;
        } catch (CkycEncryptionException ex) {
            throw ex;
        } catch (Exception e) {
            log.error("Unexpected error while preparing CKYC request", e);
            throw new CkycEncryptionException("Failed to prepare CKYC request", e);
        }
    }

    private static void validateInput(String idType, String idNo) {
        if (idType == null || idType.isBlank()) {
            throw new CkycValidationException("ID_TYPE is mandatory");
        }
        if (idNo == null || idNo.isBlank()) {
            throw new CkycValidationException("ID_NO is mandatory");
        }

        String normalizedType = idType.trim().toUpperCase();
        String normalizedNo = idNo.trim();
        Integer maxLen = ID_TYPE_MAX_LEN.get(normalizedType);
        if (maxLen == null) {
            throw new CkycValidationException("Unsupported ID_TYPE: " + normalizedType);
        }
        if (normalizedNo.length() > maxLen) {
            throw new CkycValidationException("ID_NO length exceeded for ID_TYPE " + normalizedType + ". Max length: " + maxLen);
        }

        Pattern pattern = ID_TYPE_PATTERNS.get(normalizedType);
        if (pattern != null && !pattern.matcher(normalizedNo).matches()) {
            throw new CkycValidationException("ID_NO format invalid for ID_TYPE " + normalizedType);
        }

        if ("E".equals(normalizedType) && normalizedNo.split("\\|", -1).length != 4) {
            throw new CkycValidationException("ID_NO for ID_TYPE E must be: last4Aadhaar|ApplicantName|DOB|Gender");
        }
    }

    private static String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
