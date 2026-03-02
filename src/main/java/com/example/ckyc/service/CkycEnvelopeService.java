package com.example.ckyc.service;

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
        byte[] sessionKey;
        String encryptedPid;
        String encryptedSessionKey;
        try {
            sessionKey = CryptoUtil.generateSessionKey();
            encryptedPid = CryptoUtil.encryptAES(pidData, sessionKey);
            PublicKey cersaiPublicKey = KeyLoaderUtil.loadPublicKeyFromCer(ckycProperties.getCersaiCert());
            encryptedSessionKey = CryptoUtil.encryptRSA(sessionKey, cersaiPublicKey);
        } catch (Exception ex) {
            throw new CkycEncryptionException("Failed to encrypt CKYC request payload", ex);
        }

        String unsignedXml = xmlBuilderService.buildEnvelope(
                ckycProperties.getFiCode(),
                requestId,
                ckycProperties.getVersion(),
                operationTag,
                encryptedPid,
                encryptedSessionKey
        );

        try {
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

            Document document = XmlHelper.parse(unsignedXml);
            XmlUtil.signXml(document, privateKey, cert);
            return XmlHelper.toString(document);
        } catch (Exception ex) {
            throw new CkycSignatureException("Failed to sign CKYC request payload", ex);
        }
    }
}
