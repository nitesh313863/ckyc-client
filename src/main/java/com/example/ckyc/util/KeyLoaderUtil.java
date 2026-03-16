package com.example.ckyc.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class KeyLoaderUtil {

    public static PublicKey loadPublicKeyFromCer(String path) throws Exception {
        try (InputStream fis = open(path)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return cf.generateCertificate(fis).getPublicKey();
        }
    }

    public static PrivateKey loadPrivateKeyFromPKCS12(String path, String password, String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = open(path)) {
            ks.load(is, password.toCharArray());
        }
        String resolvedAlias = resolveAlias(ks, alias);
        return (PrivateKey) ks.getKey(resolvedAlias, password.toCharArray());
    }

    public static PrivateKey loadPrivateKeyFromPem(String path) throws Exception {
        byte[] raw;
        try (InputStream is = open(path)) {
            raw = is.readAllBytes();
        }
        String pem = new String(raw, StandardCharsets.UTF_8);
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static X509Certificate loadCertificateFromPKCS12(String path, String password, String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = open(path)) {
            ks.load(is, password.toCharArray());
        }
        String resolvedAlias = resolveAlias(ks, alias);
        return (X509Certificate) ks.getCertificate(resolvedAlias);
    }

    public static X509Certificate loadCertificateFromCer(String path) throws Exception {
        try (InputStream fis = open(path)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(fis);
            return (X509Certificate) cert;
        }
    }

    private static InputStream open(String path) throws Exception {
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

        Path filePath = Path.of(normalized);
        if (Files.exists(filePath)) {
            return Files.newInputStream(filePath);
        }

        // Backward compatibility for old absolute-looking config values (e.g. /keys/file.cer)
        String fallback = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        Path fallbackPath = Path.of(fallback);
        if (Files.exists(fallbackPath)) {
            return Files.newInputStream(fallbackPath);
        }

        return new FileInputStream(normalized);
    }

    private static String resolveAlias(KeyStore keyStore, String alias) throws Exception {
        if (alias != null && !alias.isBlank()) {
            return alias;
        }
        if (keyStore.size() != 1) {
            throw new IllegalArgumentException("Keystore contains multiple entries; alias must be provided");
        }
        java.util.Enumeration<String> aliases = keyStore.aliases();
        if (!aliases.hasMoreElements()) {
            throw new IllegalArgumentException("Keystore is empty");
        }
        return aliases.nextElement();
    }
}
