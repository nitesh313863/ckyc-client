package com.example.ckyc.util;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class CkycEncrypter {

    private static final String JCE_PROVIDER = "BC";

    private static final String CERTIFICATE_TYPE = "X.509";

    private PublicKey publicKey;

    private Date certExpiryDate;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public CkycEncrypter() {
    }

    public CkycEncrypter(String publicKeyFileName) {
        try (InputStream inputStream = open(publicKeyFileName)) {
            CertificateFactory certFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE, JCE_PROVIDER);
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(inputStream);
            this.publicKey = cert.getPublicKey();
            this.certExpiryDate = cert.getNotAfter();
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize encryption module", e);
        }
    }

    public byte[] generateSessionKey() throws GeneralSecurityException {
        KeyGenerator kgen = KeyGenerator.getInstance("AES", JCE_PROVIDER);
        kgen.init(256);
        SecretKey key = kgen.generateKey();
        return key.getEncoded();
    }

    public byte[] encryptUsingPublicKey(byte[] data, String version) throws GeneralSecurityException {
        Cipher pkCipher = buildRsaCipher(version);
        pkCipher.init(Cipher.ENCRYPT_MODE, this.publicKey);
        return pkCipher.doFinal(data);
    }

    public byte[] decryptUsingPrivateKey(
            String privateKeyFileName,
            byte[] data,
            String password,
            String version
    ) throws GeneralSecurityException {
        return decryptUsingPrivateKey(privateKeyFileName, data, password, version, null);
    }

    public byte[] decryptUsingPrivateKey(
            String privateKeyFileName,
            byte[] data,
            String password,
            String version,
            String alias
    ) throws GeneralSecurityException {
        Cipher pkCipher = buildRsaCipher(version);
        DigitalSigner signer = alias == null || alias.isBlank()
                ? new DigitalSigner(privateKeyFileName, password.toCharArray())
                : new DigitalSigner(privateKeyFileName, password.toCharArray(), alias);
        PrivateKey key = signer.getKeyEntry().getPrivateKey();
        pkCipher.init(Cipher.DECRYPT_MODE, key);
        return pkCipher.doFinal(data);
    }

    public byte[] decryptUsingPrivateKey(PrivateKey privateKey, byte[] data, String version) throws GeneralSecurityException {
        Cipher pkCipher = buildRsaCipher(version);
        pkCipher.init(Cipher.DECRYPT_MODE, privateKey);
        return pkCipher.doFinal(data);
    }

    public byte[] encryptUsingSessionKey(byte[] sessionKey, byte[] data) throws InvalidCipherTextException {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new AESEngine(), new PKCS7Padding());
        cipher.init(true, new KeyParameter(sessionKey));

        int outputSize = cipher.getOutputSize(data.length);
        byte[] tempOp = new byte[outputSize];
        int processLen = cipher.processBytes(data, 0, data.length, tempOp, 0);
        int outputLen = cipher.doFinal(tempOp, processLen);

        byte[] result = new byte[processLen + outputLen];
        System.arraycopy(tempOp, 0, result, 0, result.length);
        return result;
    }

    public byte[] decryptUsingSessionKey(byte[] sessionKey, byte[] data) throws InvalidCipherTextException {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new AESEngine(), new PKCS7Padding());
        cipher.init(false, new KeyParameter(sessionKey));

        int outputSize = cipher.getOutputSize(data.length);
        byte[] tempOp = new byte[outputSize];
        int processLen = cipher.processBytes(data, 0, data.length, tempOp, 0);
        int outputLen = cipher.doFinal(tempOp, processLen);

        byte[] result = new byte[processLen + outputLen];
        System.arraycopy(tempOp, 0, result, 0, result.length);
        return result;
    }

    public String getCertificateIdentifier() {
        if (this.certExpiryDate == null) {
            return null;
        }
        SimpleDateFormat ciDateFormat = new SimpleDateFormat("yyyyMMdd");
        ciDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return ciDateFormat.format(this.certExpiryDate);
    }

    private Cipher buildRsaCipher(String version) throws GeneralSecurityException {
        String normalized = version == null ? "" : version.trim();
        String transformation;
        if ("1.1".equalsIgnoreCase(normalized) || "1.2".equalsIgnoreCase(normalized)) {
            transformation = "RSA/NONE/OAEPWithSHA1AndMGF1Padding";
        } else if ("1.3".equalsIgnoreCase(normalized)) {
            transformation = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
        } else {
            transformation = "RSA/ECB/PKCS1Padding";
        }
        return Cipher.getInstance(transformation);
    }

    private InputStream open(String path) throws IOException {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Key/certificate path must not be blank");
        }
        String normalized = path.trim();
        if (normalized.startsWith("classpath:")) {
            String resource = normalized.substring("classpath:".length());
            if (resource.startsWith("/")) {
                resource = resource.substring(1);
            }
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
            if (in == null) {
                throw new IllegalArgumentException("Classpath resource not found: " + normalized);
            }
            return in;
        }
        File file = new File(normalized);
        if (file.exists()) {
            return new FileInputStream(file);
        }
        String fallback = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        File fallbackFile = new File(fallback);
        if (fallbackFile.exists()) {
            return new FileInputStream(fallbackFile);
        }
        return new FileInputStream(normalized);
    }
}
