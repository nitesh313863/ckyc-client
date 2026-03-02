package com.example.ckyc.util;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.exception.CkycValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignatureImageValidatorTest {

    private SignatureImageValidator signatureImageValidator;

    @BeforeEach
    void setUp() {
        CkycProperties properties = new CkycProperties();
        properties.getUpload().setMaxImageBytes(1024);
        signatureImageValidator = new SignatureImageValidator(properties);
    }

    @Test
    void validate_shouldPass_forValidSignatureImage() {
        assertDoesNotThrow(() -> signatureImageValidator.validate(base64("signature-content"), "PNG", "imageList[0]"));
    }

    @Test
    void validate_shouldFail_forExecutableContent() {
        String executable = base64("<script>alert('x')</script>");
        assertThrows(CkycValidationException.class, () -> signatureImageValidator.validate(executable, "JPG", "imageList[0]"));
    }

    @Test
    void validate_shouldFail_forUnsupportedFormat() {
        assertThrows(CkycValidationException.class, () -> signatureImageValidator.validate(base64("abc"), "GIF", "imageList[0]"));
    }

    @Test
    void validate_shouldFail_forSizeExceeded() {
        CkycProperties properties = new CkycProperties();
        properties.getUpload().setMaxImageBytes(3);
        SignatureImageValidator localValidator = new SignatureImageValidator(properties);

        assertThrows(CkycValidationException.class, () -> localValidator.validate(base64("123456789"), "PNG", "imageList[0]"));
    }

    private String base64(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }
}
