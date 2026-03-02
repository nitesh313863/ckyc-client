package com.example.ckyc.serviceImpl;

import com.example.ckyc.exception.CkycValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvelopeValidationTest {

    private final XmlBuilderService xmlBuilderService = new XmlBuilderService();

    @Test
    void buildEnvelope_shouldRejectUnsupportedOperationTag() {
        assertThrows(
                CkycValidationException.class,
                () -> xmlBuilderService.buildEnvelope("FI01", "00000001", "1.3", "BAD_TAG", "pid", "key")
        );
    }

    @Test
    void buildEnvelope_shouldAllowWhitelistedOperationTag() {
        assertDoesNotThrow(
                () -> xmlBuilderService.buildEnvelope("FI01", "00000001", "1.3", "CKYC_UPDATE", "pid", "key")
        );
    }
}
