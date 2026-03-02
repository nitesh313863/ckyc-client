package com.example.ckyc.util;

import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.exception.CkycValidationException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class ImageMapperUtil {

    private static final Set<String> VALID_IMAGE_CODES = Set.of(
            "01", "02", "03", "04", "05", "06", "07", "70", "71"
    );

    public NormalizedImage normalize(CkycUpdateRequestDto.ImageDetails image) {
        if (image == null) {
            throw new CkycValidationException("Invalid image payload");
        }

        String resolvedCode = resolveImageCode(image.getImageCode());
        if (!VALID_IMAGE_CODES.contains(resolvedCode)) {
            throw new CkycValidationException(
                    "Invalid image code: " + resolvedCode + ". Allowed values: 01,02,03,04,05,06,07,70,71"
            );
        }

        String imageData = resolveImageData(image.getImageData());
        if (imageData == null || imageData.isBlank()) {
            throw new CkycValidationException("Image data is mandatory for image code: " + resolvedCode);
        }
        return new NormalizedImage(resolvedCode, imageData, normalizeFormat(image.getImageFormat()));
    }

    private String resolveImageCode(String imageCode) {
        if (imageCode != null && !imageCode.isBlank()) {
            return imageCode.trim();
        }
        throw new CkycValidationException("imageCode is mandatory");
    }

    private String resolveImageData(String imageData) {
        if (imageData != null && !imageData.isBlank()) {
            return imageData.trim();
        }
        return null;
    }

    private String normalizeFormat(String imageFormat) {
        if (imageFormat == null || imageFormat.isBlank()) {
            return null;
        }
        return imageFormat.trim().toUpperCase(Locale.ROOT);
    }

    public record NormalizedImage(String imageCode, String imageData, String imageFormat) {
    }
}
