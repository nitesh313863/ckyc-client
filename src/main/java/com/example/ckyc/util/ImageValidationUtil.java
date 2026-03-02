package com.example.ckyc.util;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.exception.CkycValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ImageValidationUtil {

    private static final Set<String> ALLOWED_IMAGE_CODES = Set.of(
            "01", "02", "03", "04", "05", "06", "07", "70", "71"
    );

    private final CkycProperties ckycProperties;
    private final SignatureImageValidator signatureImageValidator;

    public void validateUploadRequest(CkycUploadRequest request) {
        if (request.getIdentityDetails() == null || request.getIdentityDetails().isEmpty()) {
            throw new CkycValidationException("At least one identity detail is required");
        }
        if (request.getAddressDetails() == null || request.getAddressDetails().isEmpty()) {
            throw new CkycValidationException("At least one address detail is required");
        }
        if (request.getImageDetails() == null || request.getImageDetails().isEmpty()) {
            throw new CkycValidationException("At least one image is required and photograph (70) is mandatory");
        }

        Set<String> seenCodes = new HashSet<>();
        boolean photographPresent = false;
        boolean signaturePresent = false;
        for (int i = 0; i < request.getImageDetails().size(); i++) {
            CkycUploadRequest.ImageDetail image = request.getImageDetails().get(i);
            if (image == null) {
                continue;
            }
            String code = image.getImageCode();
            validateAllowedCode(code, "imageDetails[" + i + "].imageCode");
            code = code.trim();
            if (!seenCodes.add(code)) {
                throw new CkycValidationException("Duplicate imageCode not allowed: " + code);
            }
            validateBase64AndSize(image.getImageData(), "imageDetails[" + i + "].imageData");
            validateImageFormat(image.getImageData(), image.getImageFormat(), "imageDetails[" + i + "]");
            if ("70".equals(code)) {
                photographPresent = true;
            }
            if ("71".equals(code)) {
                signaturePresent = true;
                signatureImageValidator.validate(image.getImageData(), image.getImageFormat(), "imageDetails[" + i + "]");
            }
        }
        if (!photographPresent) {
            throw new CkycValidationException("Photograph imageCode 70 is mandatory for upload");
        }
        if (!signaturePresent) {
            throw new CkycValidationException("Signature imageCode 71 is mandatory for upload");
        }
    }

    public void validateUpdateImages(List<ImageMapperUtil.NormalizedImage> images) {
        if (images == null || images.isEmpty()) {
            return;
        }
        Set<String> seenCodes = new HashSet<>();
        for (int i = 0; i < images.size(); i++) {
            ImageMapperUtil.NormalizedImage image = images.get(i);
            if (image == null) {
                continue;
            }
            validateAllowedCode(image.imageCode(), "imageList[" + i + "].imageCode");
            if (!seenCodes.add(image.imageCode())) {
                throw new CkycValidationException("Duplicate imageCode not allowed: " + image.imageCode());
            }
            validateBase64AndSize(image.imageData(), "imageList[" + i + "].imageData");
            validateImageFormat(image.imageData(), image.imageFormat(), "imageList[" + i + "]");
            if ("71".equals(image.imageCode())) {
                signatureImageValidator.validate(image.imageData(), image.imageFormat(), "imageList[" + i + "]");
            }
        }
    }

    private void validateAllowedCode(String code, String field) {
        if (code == null || code.isBlank()) {
            throw new CkycValidationException(field + " is mandatory");
        }
        String normalized = code.trim();
        if (!ALLOWED_IMAGE_CODES.contains(normalized)) {
            throw new CkycValidationException(
                    "Invalid imageCode " + code + ". Allowed values: 01,02,03,04,05,06,07,70,71"
            );
        }
    }

    private void validateBase64AndSize(String data, String field) {
        if (data == null || data.isBlank()) {
            throw new CkycValidationException(field + " is mandatory");
        }
        byte[] decoded;
        try {
            decoded = java.util.Base64.getDecoder().decode(stripDataUriPrefix(data));
        } catch (IllegalArgumentException ex) {
            throw new CkycValidationException(field + " must be valid Base64");
        }
        if (decoded.length == 0) {
            throw new CkycValidationException(field + " must not be empty");
        }
        int maxImageBytes = Math.max(1, ckycProperties.getUpload().getMaxImageBytes());
        if (decoded.length > maxImageBytes) {
            throw new CkycValidationException(
                    field + " size exceeded. Max allowed bytes: " + maxImageBytes
            );
        }
    }

    private void validateImageFormat(String imageData, String imageFormat, String fieldPrefix) {
        String resolvedFormat = resolveImageFormat(imageData, imageFormat);
        if (resolvedFormat == null || resolvedFormat.isBlank()) {
            throw new CkycValidationException(fieldPrefix + ".imageFormat is mandatory and must be JPG/JPEG/PNG");
        }
        Set<String> allowedFormats = ckycProperties.getUpload().getAllowedImageFormats()
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        if (allowedFormats.isEmpty()) {
            allowedFormats = Set.of("JPG", "JPEG", "PNG");
        }
        if (!allowedFormats.contains(resolvedFormat)) {
            throw new CkycValidationException(fieldPrefix + ".imageFormat must be one of " + allowedFormats);
        }
    }

    private String resolveImageFormat(String imageData, String imageFormat) {
        if (imageFormat != null && !imageFormat.isBlank()) {
            return imageFormat.trim().toUpperCase(Locale.ROOT);
        }
        if (imageData == null) {
            return null;
        }
        String lower = imageData.toLowerCase(Locale.ROOT);
        if (lower.startsWith("data:image/jpeg;base64,")) {
            return "JPEG";
        }
        if (lower.startsWith("data:image/jpg;base64,")) {
            return "JPG";
        }
        if (lower.startsWith("data:image/png;base64,")) {
            return "PNG";
        }
        return null;
    }

    private String stripDataUriPrefix(String data) {
        int commaIndex = data.indexOf(',');
        if (commaIndex > -1 && data.substring(0, commaIndex).contains("base64")) {
            return data.substring(commaIndex + 1);
        }
        return data;
    }
}
