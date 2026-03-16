package com.example.ckyc.serviceImpl;

import com.example.ckyc.util.CkycEncrypter;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class CkycEncryptionTest {

    private static final Logger log = LoggerFactory.getLogger(CkycEncryptionTest.class);

    @Test
    void testEncryptPidAndSessionKey() throws Exception {

        log.info("========== CKYC ENCRYPTION TEST STARTED ==========");

        String certPath = "src/main/resources/keys/ckyc-public.cer";
        log.info("Loading CKYC public certificate from path: {}", certPath);

        CkycEncrypter encrypter = new CkycEncrypter(certPath);

        log.info("Certificate loaded successfully");

        String pidData = "<PID_DATA><ID_NO>123456</ID_NO></PID_DATA>";

        log.info("PID XML Data:");
        log.info("PID Length: {}", pidData.length());
        log.info("PID Value: {}", pidData);

        log.info("--------------------------------------------------");
        log.info("STEP 1: GENERATE SESSION KEY");

        byte[] sessionKey = encrypter.generateSessionKey();

        log.info("Session Key Generated Successfully");
        log.info("Session Key Length (bytes): {}", sessionKey.length);

        String sessionKeyBase64 = Base64.encodeBase64String(sessionKey);

        log.info("Session Key (Base64): {}", sessionKeyBase64);

        log.info("--------------------------------------------------");
        log.info("STEP 2: ENCRYPT PID USING SESSION KEY");

        byte[] encryptedPidBytes =
                encrypter.encryptUsingSessionKey(
                        sessionKey,
                        pidData.getBytes(StandardCharsets.UTF_8)
                );

        log.info("PID Encryption Successful");

        log.info("Encrypted PID Bytes Length: {}", encryptedPidBytes.length);

        String encryptedPid = Base64.encodeBase64String(encryptedPidBytes);

        log.info("Encrypted PID (Base64): {}", encryptedPid);

        log.info("--------------------------------------------------");
        log.info("STEP 3: ENCRYPT SESSION KEY USING CKYC PUBLIC KEY");

        byte[] encryptedSessionKeyBytes =
                encrypter.encryptUsingPublicKey(sessionKey, "1.3");

        log.info("Session Key Encryption Successful");

        log.info("Encrypted SessionKey Bytes Length: {}", encryptedSessionKeyBytes.length);

        String encryptedSessionKey =
                Base64.encodeBase64String(encryptedSessionKeyBytes);

        log.info("Encrypted SessionKey (Base64): {}", encryptedSessionKey);

        log.info("--------------------------------------------------");
        log.info("STEP 4: RUNNING ASSERTIONS");

        assertNotNull(encryptedPid);
        assertNotNull(encryptedSessionKey);

        assertFalse(encryptedPid.isEmpty());
        assertFalse(encryptedSessionKey.isEmpty());

        log.info("Assertions Passed Successfully");

        log.info("--------------------------------------------------");
        log.info("FINAL OUTPUT");

        log.info("Encrypted PID (Base64): {}", encryptedPid);
        log.info("Encrypted Session Key (Base64): {}", encryptedSessionKey);

        System.out.println("Encrypted PID: " + encryptedPid);
        System.out.println("Encrypted SessionKey: " + encryptedSessionKey);

        log.info("========== CKYC ENCRYPTION TEST COMPLETED ==========");
    }
}