package com.example.ckyc.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

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
        return (PrivateKey) ks.getKey(alias, password.toCharArray());
    }

    public static X509Certificate loadCertificateFromPKCS12(String path, String password, String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = open(path)) {
            ks.load(is, password.toCharArray());
        }
        return (X509Certificate) ks.getCertificate(alias);
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
}
