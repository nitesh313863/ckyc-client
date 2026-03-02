package com.example.ckyc.service;

import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.util.ImageMapperUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class UpdateXmlBuilder {

    public String buildPidData(
            CkycUpdateRequestDto request,
            String requestTimestamp,
            List<ImageMapperUtil.NormalizedImage> images
    ) {
        StringBuilder xml = new StringBuilder();
        xml.append("<PID_DATA>");
        xml.append(tag("DATE_TIME", requestTimestamp));
        xml.append(tag("CKYC_NO", request.getCkycNo()));
        xml.append(tag("UPDATE_TYPE", request.getUpdateType().toUpperCase(Locale.ROOT)));

        appendPersonalDetails(xml, request.getPersonalDetails());
        appendIdentityList(xml, request.getIdentityList());
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
            item.append(tag("ID_TYPE", identity.getIdType()));
            item.append(tag("ID_NUMBER", identity.getIdNumber()));
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

    private void appendImageList(StringBuilder xml, List<ImageMapperUtil.NormalizedImage> imageList) {
        if (imageList == null || imageList.isEmpty()) {
            return;
        }
        StringBuilder section = new StringBuilder();
        for (ImageMapperUtil.NormalizedImage image : imageList) {
            if (image == null) {
                continue;
            }
            section.append("<IMAGE>")
                    .append(tag("IMAGE_CODE", image.imageCode()))
                    .append(tag("IMAGE_DATA", image.imageData()))
                    .append(tag("IMAGE_FORMAT", image.imageFormat()))
                    .append("</IMAGE>");
        }
        if (!section.isEmpty()) {
            xml.append("<IMAGE_DETAILS>").append(section).append("</IMAGE_DETAILS>");
        }
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
