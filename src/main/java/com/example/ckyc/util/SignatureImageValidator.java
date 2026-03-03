package com.example.ckyc.util;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.exception.CkycValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SignatureImageValidator {

    private static final Set<String> ALLOWED_SIGNATURE_FORMATS = Set.of("JPG", "JPEG", "TIF", "TIFF");

    private final CkycProperties ckycProperties;

    public void validate(String imageData, String imageFormat, String fieldPrefix) {
        if (imageData == null || imageData.isBlank()) {
            throw new CkycValidationException(fieldPrefix + ".imageData is mandatory for IMAGE_CODE 71");
        }

        String normalizedBase64 = stripDataUriPrefix(imageData);
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(normalizedBase64);
        } catch (IllegalArgumentException ex) {
            throw new CkycValidationException(fieldPrefix + ".imageData must be valid Base64 for IMAGE_CODE 71");
        }
        if (decoded.length == 0) {
            throw new CkycValidationException(fieldPrefix + ".imageData must not be empty for IMAGE_CODE 71");
        }

        int maxImageBytes = Math.max(1, ckycProperties.getUpload().getMaxImageBytes());
        if (decoded.length > maxImageBytes) {
            throw new CkycValidationException(fieldPrefix + ".imageData size exceeded for IMAGE_CODE 71. Max bytes: " + maxImageBytes);
        }

        String resolvedFormat = resolveFormat(imageData, imageFormat);
        if (resolvedFormat == null || !ALLOWED_SIGNATURE_FORMATS.contains(resolvedFormat)) {
            throw new CkycValidationException(fieldPrefix + ".imageFormat must be JPG/JPEG/TIF/TIFF for IMAGE_CODE 71");
        }

        if (looksExecutable(decoded)) {
            throw new CkycValidationException(fieldPrefix + ".imageData contains unsafe executable content for IMAGE_CODE 71");
        }
    }

    private String resolveFormat(String imageData, String imageFormat) {
        if (imageFormat != null && !imageFormat.isBlank()) {
            return imageFormat.trim().toUpperCase(Locale.ROOT);
        }
        String lower = imageData.toLowerCase(Locale.ROOT);
        if (lower.startsWith("data:image/jpeg;base64,")) {
            return "JPEG";
        }
        if (lower.startsWith("data:image/jpg;base64,")) {
            return "JPG";
        }
        if (lower.startsWith("data:image/tiff;base64,")) {
            return "TIFF";
        }
        if (lower.startsWith("data:image/tif;base64,")) {
            return "TIF";
        }
        return null;
    }

    private String stripDataUriPrefix(String imageData) {
        int commaIndex = imageData.indexOf(',');
        if (commaIndex > -1 && imageData.substring(0, commaIndex).contains("base64")) {
            return imageData.substring(commaIndex + 1);
        }
        return imageData;
    }

    private boolean looksExecutable(byte[] decoded) {
        String normalized = new String(decoded, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return normalized.contains("<script")
                || normalized.contains("<?php")
                || normalized.contains("<!doctype html")
                || normalized.contains("<html")
                || normalized.startsWith("mz");
    }
}
