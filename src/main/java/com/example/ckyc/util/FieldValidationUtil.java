package com.example.ckyc.util;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.constant.AuthFactorType;
import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.exception.CkycValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class FieldValidationUtil {

    private static final Pattern DOB_PATTERN = Pattern.compile("^\\d{2}-\\d{2}-\\d{4}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^\\d{10}$");
    private static final Pattern CKYC_NO_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern CKYC_IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");
    private static final Pattern PIN_CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern OTP_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    private final CkycProperties ckycProperties;

    public void validateDownloadRequest(CkycDownloadRequest request) {
        if (request == null) {
            throw new CkycValidationException("Request body is mandatory");
        }
        validateDownloadIdentifier(request.getCkycNo(), "CKYC_NO");
        validateAuthFactor(request.getAuthFactorType(), request.getAuthFactor());
    }

    public void validateValidateOtpRequest(CkycValidateOtpRequest request) {
        if (request == null) {
            throw new CkycValidationException("Request body is mandatory");
        }
        if (request.getRequestId() == null || request.getRequestId().isBlank()) {
            throw new CkycValidationException("requestId is mandatory for validate-otp flow");
        }
        if (request.getValidate() != null && !request.getValidate().isBlank()
                && !"Y".equalsIgnoreCase(request.getValidate().trim())) {
            throw new CkycValidationException("VALIDATE must be Y");
        }
        if (request.getOtp() != null && !request.getOtp().isBlank()
                && !OTP_PATTERN.matcher(request.getOtp().trim()).matches()) {
            throw new CkycValidationException("OTP must be numeric 6 digits");
        }
    }

    public void validateUploadRequest(CkycUploadRequest request) {
        if (request == null) {
            throw new CkycValidationException("Request body is mandatory");
        }
        CkycUploadRequest.PersonalDetails personal = request.getPersonalDetails();
        if (personal == null) {
            throw new CkycValidationException("personalDetails is mandatory");
        }
        validateDob(personal.getDob(), "personalDetails.dob", true);
        validatePan(personal.getPan(), "personalDetails.pan", false);
        validateMobile(personal.getMobile(), "personalDetails.mobile", false);
        validateEmail(personal.getEmail(), "personalDetails.email", false);
        validateRelatedPersons(request.getRelatedPersons(), "relatedPersons");
    }

    public void validateUpdateRequest(CkycUpdateRequestDto request) {
        if (request == null) {
            throw new CkycValidationException("Request body is mandatory");
        }
        validateCkycNo(request.getCkycNo(), "CKYC_NO");

        CkycUpdateRequestDto.PersonalDetails personal = request.getPersonalDetails();
        if (personal != null) {
            validateDob(personal.getDob(), "personalDetails.dob", false);
            validatePan(personal.getPan(), "personalDetails.pan", false);
            validateAadhaar(personal.getAadhaar(), "personalDetails.aadhaar", false);
            validateMobile(personal.getMobile(), "personalDetails.mobile", false);
            validateEmail(personal.getEmail(), "personalDetails.email", false);
        }
        validateRelatedPersons(request.getRelatedPersons(), "relatedPersons");
    }

    public void validateCkycNo(String ckycNo, String fieldName) {
        if (ckycNo == null || ckycNo.isBlank()) {
            throw new CkycValidationException(fieldName + " is mandatory");
        }
        String normalized = ckycNo.trim();
        int expectedLength = Math.max(1, ckycProperties.getValidation().getCkycNoLength());
        if (normalized.length() != expectedLength || !CKYC_NO_PATTERN.matcher(normalized).matches()) {
            throw new CkycValidationException(fieldName + " must be numeric and " + expectedLength + " digits");
        }
    }

    private void validateDownloadIdentifier(String identifier, String fieldName) {
        if (identifier == null || identifier.isBlank()) {
            throw new CkycValidationException(fieldName + " is mandatory");
        }
        String normalized = identifier.trim();
        int expectedLength = Math.max(1, ckycProperties.getValidation().getCkycNoLength());
        if (normalized.length() != expectedLength || !CKYC_IDENTIFIER_PATTERN.matcher(normalized).matches()) {
            throw new CkycValidationException(fieldName + " must be " + expectedLength + " alphanumeric characters");
        }
    }

    private void validateAuthFactor(AuthFactorType authFactorType, String authFactor) {
        if (authFactorType == null) {
            throw new CkycValidationException("AUTH_FACTOR_TYPE is mandatory");
        }
        if (authFactor == null || authFactor.isBlank()) {
            throw new CkycValidationException("AUTH_FACTOR is mandatory");
        }
        String value = authFactor.trim();

        switch (authFactorType) {
            case DOI -> validateDob(value, "AUTH_FACTOR", true);
            case MOBILE -> validateMobile(value, "AUTH_FACTOR", true);
            case EMAIL -> validateEmail(value, "AUTH_FACTOR", true);
            case PINCODE -> {
                if (!PIN_CODE_PATTERN.matcher(value).matches()) {
                    throw new CkycValidationException("AUTH_FACTOR must be valid 6-digit pincode for AUTH_FACTOR_TYPE 05");
                }
            }
            default -> throw new CkycValidationException("Unsupported AUTH_FACTOR_TYPE: " + authFactorType.name());
        }
    }

    private void validateDob(String value, String fieldName, boolean mandatory) {
        if (value == null || value.isBlank()) {
            if (mandatory) {
                throw new CkycValidationException(fieldName + " is mandatory");
            }
            return;
        }
        if (!DOB_PATTERN.matcher(value.trim()).matches()) {
            throw new CkycValidationException(fieldName + " must be in dd-MM-yyyy format");
        }
    }

    private void validatePan(String value, String fieldName, boolean mandatory) {
        if (value == null || value.isBlank()) {
            if (mandatory) {
                throw new CkycValidationException(fieldName + " is mandatory");
            }
            return;
        }
        if (!PAN_PATTERN.matcher(value.trim().toUpperCase(Locale.ROOT)).matches()) {
            throw new CkycValidationException(fieldName + " must be valid PAN format");
        }
    }

    private void validateAadhaar(String value, String fieldName, boolean mandatory) {
        if (value == null || value.isBlank()) {
            if (mandatory) {
                throw new CkycValidationException(fieldName + " is mandatory");
            }
            return;
        }
        if (!AADHAAR_PATTERN.matcher(value.trim()).matches()) {
            throw new CkycValidationException(fieldName + " must be 12 digits");
        }
    }

    private void validateMobile(String value, String fieldName, boolean mandatory) {
        if (value == null || value.isBlank()) {
            if (mandatory) {
                throw new CkycValidationException(fieldName + " is mandatory");
            }
            return;
        }
        if (!MOBILE_PATTERN.matcher(value.trim()).matches()) {
            throw new CkycValidationException(fieldName + " must be 10 digits");
        }
    }

    private void validateEmail(String value, String fieldName, boolean mandatory) {
        if (value == null || value.isBlank()) {
            if (mandatory) {
                throw new CkycValidationException(fieldName + " is mandatory");
            }
            return;
        }
        if (!EMAIL_PATTERN.matcher(value.trim()).matches()) {
            throw new CkycValidationException(fieldName + " must be valid email");
        }
    }

    private void validateRelatedPersons(List<?> relatedPersons, String fieldName) {
        if (relatedPersons == null || relatedPersons.isEmpty()) {
            return;
        }
        for (int i = 0; i < relatedPersons.size(); i++) {
            Object person = relatedPersons.get(i);
            if (person instanceof CkycUploadRequest.RelatedPerson uploadPerson) {
                validateDob(uploadPerson.getDob(), fieldName + "[" + i + "].dob", false);
            } else if (person instanceof CkycUpdateRequestDto.RelatedPerson updatePerson) {
                validateDob(updatePerson.getDob(), fieldName + "[" + i + "].dob", false);
            }
        }
    }
}
