package com.example.ckyc.util;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MaskingUtil {

    private static final Pattern PAN_PATTERN = Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");

    public String maskSensitive(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String maskedPan = replaceByPattern(text, PAN_PATTERN, this::maskPan);
        return replaceByPattern(maskedPan, AADHAAR_PATTERN, this::maskAadhaar);
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
