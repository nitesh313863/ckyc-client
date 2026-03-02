package com.example.ckyc.util;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.constant.AuthFactorType;
import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.exception.CkycValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldValidationUtilTest {

    private FieldValidationUtil fieldValidationUtil;

    @BeforeEach
    void setUp() {
        CkycProperties properties = new CkycProperties();
        properties.getValidation().setCkycNoLength(14);
        fieldValidationUtil = new FieldValidationUtil(properties);
    }

    @Test
    void validateDownloadRequest_shouldPass_forValidInput() {
        CkycDownloadRequest request = new CkycDownloadRequest();
        request.setCkycNo("12345678901234");
        request.setAuthFactorType(AuthFactorType.PAN);
        request.setAuthFactor("ABCDE1234F");

        assertDoesNotThrow(() -> fieldValidationUtil.validateDownloadRequest(request));
    }

    @Test
    void validateDownloadRequest_shouldFail_forInvalidCkycNo() {
        CkycDownloadRequest request = new CkycDownloadRequest();
        request.setCkycNo("ABC");
        request.setAuthFactorType(AuthFactorType.OTP);
        request.setAuthFactor("123456");

        assertThrows(CkycValidationException.class, () -> fieldValidationUtil.validateDownloadRequest(request));
    }

    @Test
    void validateUploadRequest_shouldFail_forInvalidEmail() {
        CkycUploadRequest request = new CkycUploadRequest();
        CkycUploadRequest.PersonalDetails personal = new CkycUploadRequest.PersonalDetails();
        personal.setDob("11-01-1995");
        personal.setEmail("invalid_email");
        request.setPersonalDetails(personal);

        assertThrows(CkycValidationException.class, () -> fieldValidationUtil.validateUploadRequest(request));
    }

    @Test
    void validateUpdateRequest_shouldFail_forInvalidMobile() {
        CkycUpdateRequestDto request = new CkycUpdateRequestDto();
        request.setCkycNo("12345678901234");
        request.setUpdateType("PARTIAL");
        CkycUpdateRequestDto.PersonalDetails personal = new CkycUpdateRequestDto.PersonalDetails();
        personal.setMobile("98765");
        request.setPersonalDetails(personal);

        assertThrows(CkycValidationException.class, () -> fieldValidationUtil.validateUpdateRequest(request));
    }
}
