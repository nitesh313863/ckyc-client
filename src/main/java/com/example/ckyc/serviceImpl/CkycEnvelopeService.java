package com.example.ckyc.serviceImpl;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.exception.CkycEncryptionException;
import com.example.ckyc.exception.CkycSignatureException;
import com.example.ckyc.util.CryptoUtil;
import com.example.ckyc.util.KeyLoaderUtil;
import com.example.ckyc.util.XmlHelper;
import com.example.ckyc.util.XmlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

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
            sessionKey = CryptoUtil.generateSessionKey();
            encryptedPid = CryptoUtil.encryptAES(pidData, sessionKey);
            PublicKey cersaiPublicKey = KeyLoaderUtil.loadPublicKeyFromCer(resolveCkycPublicCertPath());
            encryptedSessionKey = CryptoUtil.encryptRSA(sessionKey, cersaiPublicKey);
        } catch (Exception ex) {
            throw new CkycEncryptionException("Failed to encrypt CKYC request payload", ex);
        }

        String unsignedXml = envelopeBuilder.build(encryptedPid, encryptedSessionKey);

        try {
            PrivateKey privateKey = loadProjectPrivateKey();
            X509Certificate cert = loadProjectCertificate();

            Document document = XmlHelper.parse(unsignedXml);
            XmlUtil.signXml(document, privateKey, cert);
            return XmlHelper.toString(document);
        } catch (Exception ex) {
            throw new CkycSignatureException("Failed to sign CKYC request payload", ex);
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

    private X509Certificate loadProjectCertificate() throws Exception {
        if (hasText(ckycProperties.getProjectPublicKeyPath())) {
            return KeyLoaderUtil.loadCertificateFromCer(ckycProperties.getProjectPublicKeyPath());
        }
        return KeyLoaderUtil.loadCertificateFromPKCS12(
                ckycProperties.getP12Path(),
                ckycProperties.getP12Password(),
                ckycProperties.getP12Alias()
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
