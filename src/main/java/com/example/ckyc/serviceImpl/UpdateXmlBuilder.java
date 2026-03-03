package com.example.ckyc.serviceImpl;

import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.exception.CkycValidationException;
import com.example.ckyc.util.ImageMapperUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class UpdateXmlBuilder {

    private static final Pattern CKYC_NO_PATTERN = Pattern.compile("^\\d{14}$");

    public String buildPidData(
            CkycUpdateRequestDto request,
            String requestTimestamp,
            List<ImageMapperUtil.NormalizedImage> images
    ) {
        String updateType = normalizeUpdateType(request.getUpdateType());
        String ckycNo = normalizeCkycNo(request.getCkycNo());

        StringBuilder xml = new StringBuilder();
        xml.append("<PID_DATA>");
        xml.append(tag("DATE_TIME", requestTimestamp));
        xml.append(tag("CKYC_NO", ckycNo));
        xml.append(tag("UPDATE_TYPE", updateType));

        appendPersonalDetails(xml, request.getPersonalDetails());
        appendIdentityList(xml, request.getIdentityList());
        appendAddressDetails(xml, request.getAddressDetails());
        appendImageList(xml, images);

        xml.append("</PID_DATA>");
        return xml.toString();
    }

    public String buildUnsignedRequest(
            String fiCode,
            String requestId,
            String version,
            String encryptedSessionKey,
            String encryptedPid
    ) {
        return "<CKYC_UPDATE_REQUEST>"
                + "<HEADER>"
                + tag("FI_CODE", fiCode)
                + tag("REQUEST_ID", requestId)
                + tag("VERSION", version)
                + "</HEADER>"
                + "<CKYC_INQ>"
                + tag("SESSION_KEY", encryptedSessionKey)
                + tag("PID", encryptedPid)
                + "</CKYC_INQ>"
                + "</CKYC_UPDATE_REQUEST>";
    }

    private void appendPersonalDetails(StringBuilder xml, CkycUpdateRequestDto.PersonalDetails details) {
        if (details == null) {
            return;
        }
        StringBuilder section = new StringBuilder();
        section.append(tag("FIRST_NAME", details.getFirstName()));
        section.append(tag("MIDDLE_NAME", details.getMiddleName()));
        section.append(tag("LAST_NAME", details.getLastName()));
        section.append(tag("DOB", details.getDob()));
        section.append(tag("GENDER", details.getGender()));
        section.append(tag("PAN", details.getPan()));
        section.append(tag("AADHAAR", details.getAadhaar()));
        section.append(tag("MOBILE", details.getMobile()));
        section.append(tag("EMAIL", details.getEmail()));
        if (!section.isEmpty()) {
            xml.append("<PERSONAL_DETAILS>").append(section).append("</PERSONAL_DETAILS>");
        }
    }

    private void appendIdentityList(StringBuilder xml, List<CkycUpdateRequestDto.IdentityDetails> identityList) {
        if (identityList == null || identityList.isEmpty()) {
            return;
        }
        StringBuilder section = new StringBuilder();
        for (CkycUpdateRequestDto.IdentityDetails identity : identityList) {
            if (identity == null) {
                continue;
            }
            StringBuilder item = new StringBuilder();
            item.append(tag("IDENTITY_TYPE", identity.getIdType()));
            item.append(tag("IDENTITY_NUMBER", identity.getIdNumber()));
            item.append(tag("ISSUE_DATE", identity.getIssueDate()));
            item.append(tag("EXPIRY_DATE", identity.getExpiryDate()));
            if (!item.isEmpty()) {
                section.append("<IDENTITY>").append(item).append("</IDENTITY>");
            }
        }
        if (!section.isEmpty()) {
            xml.append("<IDENTITY_DETAILS>").append(section).append("</IDENTITY_DETAILS>");
        }
    }

    private void appendAddressDetails(StringBuilder xml, CkycUpdateRequestDto.AddressDetails details) {
        if (details == null) {
            return;
        }
        StringBuilder item = new StringBuilder();
        item.append(tag("ADDRESS_TYPE", details.getAddressType()));
        item.append(tag("LINE1", details.getLine1()));
        item.append(tag("LINE2", details.getLine2()));
        item.append(tag("CITY", details.getCity()));
        item.append(tag("DISTRICT", details.getDistrict()));
        item.append(tag("STATE", details.getState()));
        item.append(tag("PIN_CODE", details.getPinCode()));
        item.append(tag("COUNTRY", details.getCountry()));
        if (!item.isEmpty()) {
            xml.append("<ADDRESS_DETAILS>")
                    .append("<ADDRESS_DETAIL>")
                    .append(item)
                    .append("</ADDRESS_DETAIL>")
                    .append("</ADDRESS_DETAILS>");
        }
    }

    private void appendImageList(StringBuilder xml, List<ImageMapperUtil.NormalizedImage> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            return;
        }
        StringBuilder section = new StringBuilder();
        for (ImageMapperUtil.NormalizedImage image : imageList) {
            if (image == null) {
                continue;
            }
            StringBuilder item = new StringBuilder();
            item.append(tag("IMAGE_CODE", image.imageCode()));
            item.append(tag("IMAGE_DATA", image.imageData()));
            item.append(tag("IMAGE_FORMAT", image.imageFormat()));
            if (!item.isEmpty()) {
                section.append("<IMAGE>").append(item).append("</IMAGE>");
            }
        }
        if (!section.isEmpty()) {
            xml.append("<IMAGE_DETAILS>").append(section).append("</IMAGE_DETAILS>");
        }
    }

    private String normalizeUpdateType(String updateType) {
        if (updateType == null || updateType.isBlank()) {
            throw new CkycValidationException("updateType is mandatory");
        }
        String normalized = updateType.trim().toUpperCase(Locale.ROOT);
        if (!"FULL".equals(normalized) && !"PARTIAL".equals(normalized)) {
            throw new CkycValidationException("updateType must be FULL or PARTIAL");
        }
        return normalized;
    }

    private String normalizeCkycNo(String ckycNo) {
        if (ckycNo == null || ckycNo.isBlank()) {
            throw new CkycValidationException("CKYC_NO is mandatory");
        }
        String normalized = ckycNo.trim();
        if (!CKYC_NO_PATTERN.matcher(normalized).matches()) {
            throw new CkycValidationException("CKYC_NO must be numeric and 14 digits");
        }
        return normalized;
    }

    private String tag(String name, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "<" + name + ">" + xmlEscape(value) + "</" + name + ">";
    }

    private String xmlEscape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
