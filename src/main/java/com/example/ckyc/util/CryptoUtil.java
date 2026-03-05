package com.example.ckyc.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class CryptoUtil {

    // CKYC samples typically use plain AES encrypted payload without explicit IV tag.
    private static final String AES_ALGO = "AES/ECB/PKCS5Padding";
    private static final String[] RSA_OAEP_ALGOS = {
            "RSA/ECB/OAEPWithSHA-256AndMGF1Padding",
            "RSA/ECB/OAEPWithSHA256AndMGF1Padding"
    };
    private static final String RSA_OAEP_FALLBACK_ALGO = "RSA/ECB/OAEPPadding";
    private static final OAEPParameterSpec RSA_OAEP_PARAMS = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );

    public static byte[] generateSessionKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey().getEncoded();
    }

    public static String encryptAES(String data, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decryptAES(String encryptedDataBase64, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] encrypted = Base64.getDecoder().decode(encryptedDataBase64);
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static String encryptRSA(byte[] sessionKey, PublicKey publicKey) throws Exception {
        Cipher cipher = getRsaCipher(Cipher.ENCRYPT_MODE, publicKey, null);
        return Base64.getEncoder().encodeToString(cipher.doFinal(sessionKey));
    }

    public static byte[] decryptRSA(String encryptedSessionKeyBase64, PrivateKey privateKey) throws Exception {
        Cipher cipher = getRsaCipher(Cipher.DECRYPT_MODE, null, privateKey);
        byte[] encryptedSessionKey = Base64.getDecoder().decode(encryptedSessionKeyBase64);
        return cipher.doFinal(encryptedSessionKey);
    }

    private static Cipher getRsaCipher(int mode, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        Exception last = null;
        for (String algo : RSA_OAEP_ALGOS) {
            try {
                Cipher cipher = Cipher.getInstance(algo);
                if (mode == Cipher.ENCRYPT_MODE) {
                    cipher.init(mode, publicKey);
                } else {
                    cipher.init(mode, privateKey);
                }
                return cipher;
            } catch (Exception ex) {
                last = ex;
            }
        }

        Cipher fallback = Cipher.getInstance(RSA_OAEP_FALLBACK_ALGO);
        if (mode == Cipher.ENCRYPT_MODE) {
            fallback.init(mode, publicKey, RSA_OAEP_PARAMS);
        } else {
            fallback.init(mode, privateKey, RSA_OAEP_PARAMS);
        }
        return fallback;
    }

    public static String randomNumeric(int digits) {
        if (digits <= 0) {
            throw new IllegalArgumentException("digits must be > 0");
        }
        SecureRandom random = new SecureRandom();
        StringBuilder out = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            out.append(random.nextInt(10));
        }
        return out.toString();
    }
}
