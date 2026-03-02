package com.example.ckyc.util;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MaskingUtil {

    private static final Pattern PAN_PATTERN = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("\\b\\d{10}\\b");
    private static final Pattern IMAGE_DATA_TAG_PATTERN = Pattern.compile("(<IMAGE_DATA>)([^<]+)(</IMAGE_DATA>)");
    private static final Pattern BASE64_URI_PATTERN = Pattern.compile("data:image/[^;]+;base64,[A-Za-z0-9+/=\\r\\n]+");
    private static final Pattern LONG_BASE64_PATTERN = Pattern.compile("\\b[A-Za-z0-9+/=]{120,}\\b");

    public String maskSensitive(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String maskedPan = replaceByPattern(text, PAN_PATTERN, this::maskPan);
        String maskedAadhaar = replaceByPattern(maskedPan, AADHAAR_PATTERN, this::maskAadhaar);
        String maskedMobile = replaceByPattern(maskedAadhaar, MOBILE_PATTERN, this::maskMobile);
        String maskedImageData = IMAGE_DATA_TAG_PATTERN.matcher(maskedMobile).replaceAll("$1[MASKED_BASE64]$3");
        String maskedDataUri = BASE64_URI_PATTERN.matcher(maskedImageData).replaceAll("data:image/[MASKED];base64,[MASKED_BASE64]");
        return LONG_BASE64_PATTERN.matcher(maskedDataUri).replaceAll("[MASKED_BASE64]");
    }

    public String maskPan(String pan) {
        if (pan == null || pan.length() != 10) {
            return pan;
        }
        return pan.substring(0, 2) + "******" + pan.substring(8);
    }

    public String maskAadhaar(String aadhaarRaw) {
        if (aadhaarRaw == null) {
            return null;
        }
        String digits = aadhaarRaw.replaceAll("\\D", "");
        if (digits.length() != 12) {
            return aadhaarRaw;
        }
        String masked = "XXXXXXXX" + digits.substring(8);
        if (aadhaarRaw.contains("-")) {
            return masked.substring(0, 4) + "-" + masked.substring(4, 8) + "-" + masked.substring(8);
        }
        if (aadhaarRaw.contains(" ")) {
            return masked.substring(0, 4) + " " + masked.substring(4, 8) + " " + masked.substring(8);
        }
        return masked;
    }

    public String maskMobile(String mobile) {
        if (mobile == null) {
            return null;
        }
        String digits = mobile.replaceAll("\\D", "");
        if (digits.length() != 10) {
            return mobile;
        }
        return "XXXXXXXX" + digits.substring(8);
    }

    private String replaceByPattern(String input, Pattern pattern, java.util.function.Function<String, String> maskFn) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(maskFn.apply(matcher.group())));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
