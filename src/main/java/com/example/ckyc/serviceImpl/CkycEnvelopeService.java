package com.example.ckyc.serviceImpl;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.exception.CkycEncryptionException;
import com.example.ckyc.exception.CkycSignatureException;
import com.example.ckyc.util.CkycEncrypter;
import com.example.ckyc.util.DigitalSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;

@Service
@RequiredArgsConstructor
public class CkycEnvelopeService {

    private final CkycProperties ckycProperties;
    private final XmlBuilderService xmlBuilderService;

    public String encryptAndSign(String operationTag, String requestId, String pidData) {
        return encryptAndSignInternal(requestId, pidData, (encryptedPid, encryptedSessionKey) ->
                xmlBuilderService.buildEnvelope(
                        ckycProperties.getFiCode(),
                        requestId,
                        ckycProperties.getVersion(),
                        operationTag,
                        encryptedPid,
                        encryptedSessionKey
                ));
    }

    public String encryptAndSignDownloadInq(String requestId, String pidData) {
        return encryptAndSignInternal(requestId, pidData, (encryptedPid, encryptedSessionKey) ->
                xmlBuilderService.buildDownloadEnvelope(
                        ckycProperties.getFiCode(),
                        requestId,
                        ckycProperties.getVersion(),
                        encryptedPid,
                        encryptedSessionKey
                ));
    }

    private String encryptAndSignInternal(String requestId, String pidData, EnvelopeBuilder envelopeBuilder) {
        byte[] sessionKey;
        String encryptedPid;
        String encryptedSessionKey;
        try {
            CkycEncrypter encrypter = new CkycEncrypter(resolveCkycPublicCertPath());
            sessionKey = encrypter.generateSessionKey();
            byte[] encryptedPidBytes = encrypter.encryptUsingSessionKey(
                    sessionKey,
                    pidData.getBytes(StandardCharsets.UTF_8)
            );
            encryptedPid = Base64.encodeBase64String(encryptedPidBytes);

            byte[] encryptedSessionKeyBytes = encrypter.encryptUsingPublicKey(
                    sessionKey,
                    ckycProperties.getVersion()
            );
            encryptedSessionKey = Base64.encodeBase64String(encryptedSessionKeyBytes);
        } catch (Exception ex) {
            throw new CkycEncryptionException("Failed to encrypt CKYC request payload", ex);
        }

        String unsignedXml = envelopeBuilder.build(encryptedPid, encryptedSessionKey);

        try {
            DigitalSigner signer = buildSigner();
            return signer.signXML(unsignedXml, true, ckycProperties.getVersion());
        } catch (Exception ex) {
            throw new CkycSignatureException("Failed to sign CKYC request payload", ex);
        }
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

    @FunctionalInterface
    private interface EnvelopeBuilder {
        String build(String encryptedPid, String encryptedSessionKey);
    }
}
