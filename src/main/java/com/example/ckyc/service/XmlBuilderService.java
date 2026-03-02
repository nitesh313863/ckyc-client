package com.example.ckyc.service;

import com.example.ckyc.dto.CkycDownloadRequest;
import com.example.ckyc.dto.CkycUpdateRequest;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.dto.CkycValidateOtpRequest;
import com.example.ckyc.exception.CkycValidationException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class XmlBuilderService {

    private static final Set<String> ALLOWED_OPERATION_TAGS = Set.of(
            "CKYC_INQ",
            "CKYC_UPLOAD",
            "CKYC_UPDATE",
            "CKYC_DOWNLOAD",
            "CKYC_VALIDATE_OTP"
    );

    public String buildDownloadPidData(CkycDownloadRequest request, String timestamp) {
        return "<PID_DATA>"
                + tag("DATE_TIME", timestamp)
                + tag("CKYC_NO", request.getCkycNo())
                + tag("AUTH_FACTOR_TYPE", request.getAuthFactorType())
                + tag("AUTH_FACTOR", request.getAuthFactor())
                + "</PID_DATA>";
    }

    public String buildValidateOtpPidData(CkycValidateOtpRequest request, String timestamp) {
        return "<PID_DATA>"
                + tag("DATE_TIME", timestamp)
                + tag("CKYC_NO", request.getCkycNo())
                + tag("OTP", request.getOtp())
                + "</PID_DATA>";
    }

    public String buildUploadPidData(CkycUploadRequest request, String timestamp) {
        StringBuilder xml = new StringBuilder();
        xml.append("<PID_DATA>");
        xml.append(tag("DATE_TIME", timestamp));

        CkycUploadRequest.PersonalDetails personal = request.getPersonalDetails();
        xml.append("<PERSONAL_DETAILS>")
                .append(tag("FIRST_NAME", personal.getFirstName()))
                .append(tag("MIDDLE_NAME", personal.getMiddleName()))
                .append(tag("LAST_NAME", personal.getLastName()))
                .append(tag("DOB", personal.getDob()))
                .append(tag("GENDER", personal.getGender()))
                .append(tag("PAN", personal.getPan()))
                .append(tag("MOBILE", personal.getMobile()))
                .append(tag("EMAIL", personal.getEmail()))
                .append("</PERSONAL_DETAILS>");

        xml.append("<IDENTITY_DETAILS>");
        for (CkycUploadRequest.IdentityDetail identity : request.getIdentityDetails()) {
            xml.append("<IDENTITY>")
                    .append(tag("IDENTITY_TYPE", identity.getIdentityType()))
                    .append(tag("IDENTITY_NUMBER", identity.getIdentityNumber()))
                    .append(tag("EXPIRY_DATE", identity.getExpiryDate()))
                    .append("</IDENTITY>");
        }
        xml.append("</IDENTITY_DETAILS>");

        xml.append("<ADDRESS_DETAILS>");
        for (CkycUploadRequest.AddressDetail address : request.getAddressDetails()) {
            xml.append("<ADDRESS_DETAIL>")
                    .append(tag("ADDRESS_TYPE", address.getAddressType()))
                    .append(tag("LINE1", address.getLine1()))
                    .append(tag("LINE2", address.getLine2()))
                    .append(tag("CITY", address.getCity()))
                    .append(tag("STATE", address.getState()))
                    .append(tag("PIN_CODE", address.getPinCode()))
                    .append(tag("COUNTRY", address.getCountry()))
                    .append("</ADDRESS_DETAIL>");
        }
        xml.append("</ADDRESS_DETAILS>");

        xml.append("<IMAGE_DETAILS>");
        for (CkycUploadRequest.ImageDetail imageDetail : request.getImageDetails()) {
            xml.append("<IMAGE>")
                    .append(tag("IMAGE_CODE", imageDetail.getImageCode()))
                    .append(tag("IMAGE_DATA", imageDetail.getImageData()))
                    .append(tag("IMAGE_FORMAT", imageDetail.getImageFormat()))
                    .append(tag("SEQUENCE_NO", imageDetail.getSequenceNo()))
                    .append("</IMAGE>");
        }
        xml.append("</IMAGE_DETAILS>");

        xml.append("<RELATED_PERSONS>");
        for (CkycUploadRequest.RelatedPerson relatedPerson : request.getRelatedPersons()) {
            xml.append("<RELATED_PERSON>")
                    .append(tag("RELATION_TYPE", relatedPerson.getRelationType()))
                    .append(tag("NAME", relatedPerson.getName()))
                    .append(tag("DOB", relatedPerson.getDob()))
                    .append("</RELATED_PERSON>");
        }
        xml.append("</RELATED_PERSONS>");

        xml.append("</PID_DATA>");
        return xml.toString();
    }

    public String buildUpdatePidData(CkycUpdateRequest request, String timestamp) {
        Map<String, String> personalFields = new LinkedHashMap<>();
        Map<String, String> identityFields = new LinkedHashMap<>();
        Map<String, String> imageFields = new LinkedHashMap<>();

        request.getChangedFields().forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null || value.isBlank()) {
                return;
            }
            if (key.startsWith("PERSONAL_DETAILS.")) {
                personalFields.put(key.substring("PERSONAL_DETAILS.".length()), value);
            } else if (key.startsWith("IDENTITY_DETAILS.")) {
                identityFields.put(key.substring("IDENTITY_DETAILS.".length()), value);
            } else if (key.startsWith("IMAGE_DETAILS.")) {
                imageFields.put(key.substring("IMAGE_DETAILS.".length()), value);
            }
        });

        StringBuilder xml = new StringBuilder();
        xml.append("<PID_DATA>")
                .append(tag("DATE_TIME", timestamp))
                .append(tag("CKYC_NO", request.getCkycNo()))
                .append(tag("UPDATE_TYPE", "PARTIAL"));

        if (!personalFields.isEmpty()) {
            xml.append("<PERSONAL_DETAILS>");
            for (Map.Entry<String, String> entry : personalFields.entrySet()) {
                xml.append(tag(safeTagName(entry.getKey()), entry.getValue()));
            }
            xml.append("</PERSONAL_DETAILS>");
        }

        if (!identityFields.isEmpty()) {
            xml.append("<IDENTITY_DETAILS><IDENTITY>");
            for (Map.Entry<String, String> entry : identityFields.entrySet()) {
                xml.append(tag(safeTagName(entry.getKey()), entry.getValue()));
            }
            xml.append("</IDENTITY></IDENTITY_DETAILS>");
        }

        if (!imageFields.isEmpty()) {
            xml.append("<IMAGE_DETAILS><IMAGE>");
            for (Map.Entry<String, String> entry : imageFields.entrySet()) {
                xml.append(tag(safeTagName(entry.getKey()), entry.getValue()));
            }
            xml.append("</IMAGE></IMAGE_DETAILS>");
        }

        xml.append("</PID_DATA>");
        return xml.toString();
    }

    public String buildEnvelope(
            String fiCode,
            String requestId,
            String version,
            String operationTag,
            String encryptedPid,
            String encryptedSessionKey
    ) {
        if (operationTag == null || operationTag.isBlank()) {
            throw new CkycValidationException("operationTag is mandatory");
        }
        String normalizedOperationTag = operationTag.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_OPERATION_TAGS.contains(normalizedOperationTag)) {
            throw new CkycValidationException("Unsupported operationTag: " + operationTag);
        }
        if (encryptedPid == null || encryptedPid.isBlank()) {
            throw new CkycValidationException("Encrypted PID is mandatory");
        }
        if (encryptedSessionKey == null || encryptedSessionKey.isBlank()) {
            throw new CkycValidationException("Encrypted session key is mandatory");
        }

        return "<REQ_ROOT>"
                + "<HEADER>"
                + tag("FI_CODE", fiCode)
                + tag("REQUEST_ID", requestId)
                + tag("VERSION", version)
                + "</HEADER>"
                + "<" + normalizedOperationTag + ">"
                + tag("PID", encryptedPid)
                + tag("SESSION_KEY", encryptedSessionKey)
                + "</" + normalizedOperationTag + ">"
                + "</REQ_ROOT>";
    }

    private String tag(String name, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "<" + name + ">" + xmlEscape(value) + "</" + name + ">";
    }

    private String xmlEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String safeTagName(String value) {
        if (value == null) {
            throw new CkycValidationException("Invalid tag name");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        if (normalized.isBlank()) {
            throw new CkycValidationException("Invalid tag name");
        }
        return normalized;
    }
}
