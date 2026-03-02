package com.example.ckyc.util;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.exception.CkycValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ImageValidationUtil {

    private static final Set<String> ALLOWED_IMAGE_CODES = Set.of(
            "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "70", "71"
    );

    private final CkycProperties ckycProperties;

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
            if ("70".equals(code)) {
                photographPresent = true;
            }
        }
        if (!photographPresent) {
            throw new CkycValidationException("Photograph imageCode 70 is mandatory for upload");
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
        }
    }

    private void validateAllowedCode(String code, String field) {
        if (code == null || code.isBlank()) {
            throw new CkycValidationException(field + " is mandatory");
        }
        if (!ALLOWED_IMAGE_CODES.contains(code)) {
            throw new CkycValidationException(
                    "Invalid imageCode " + code + ". Allowed values: 01,02,03,04,05,06,07,08,09,10,70,71"
            );
        }
    }

    private void validateBase64AndSize(String data, String field) {
        if (data == null || data.isBlank()) {
            throw new CkycValidationException(field + " is mandatory");
        }
        byte[] decoded;
        try {
            decoded = java.util.Base64.getDecoder().decode(data);
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
}
