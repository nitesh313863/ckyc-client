package com.example.ckyc.util;

import com.example.ckyc.config.CkycProperties;
import com.example.ckyc.dto.CkycUploadRequest;
import com.example.ckyc.exception.CkycValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageValidationUtilTest {

    private ImageValidationUtil imageValidationUtil;

    @BeforeEach
    void setUp() {
        CkycProperties properties = new CkycProperties();
        properties.getUpload().setMaxImageBytes(1024 * 1024);
        SignatureImageValidator signatureImageValidator = new SignatureImageValidator(properties);
        imageValidationUtil = new ImageValidationUtil(properties, signatureImageValidator);
    }

    @Test
    void validateUploadRequest_shouldPass_forValidImages() {
        CkycUploadRequest request = validUploadRequest();
        assertDoesNotThrow(() -> imageValidationUtil.validateUploadRequest(request));
    }

    @Test
    void validateUploadRequest_shouldFail_whenDuplicateImageCode() {
        CkycUploadRequest request = validUploadRequest();
        CkycUploadRequest.ImageDetail duplicate = new CkycUploadRequest.ImageDetail();
        duplicate.setImageCode("70");
        duplicate.setImageData(base64("photo-2"));
        duplicate.setImageFormat("JPG");
        List<CkycUploadRequest.ImageDetail> images = new ArrayList<>(request.getImageDetails());
        images.add(duplicate);
        request.setImageDetails(images);

        assertThrows(CkycValidationException.class, () -> imageValidationUtil.validateUploadRequest(request));
    }

    @Test
    void validateUploadRequest_shouldFail_whenSignatureMissing() {
        CkycUploadRequest request = validUploadRequest();
        request.setImageDetails(request.getImageDetails().stream().filter(image -> !"71".equals(image.getImageCode())).toList());

        assertThrows(CkycValidationException.class, () -> imageValidationUtil.validateUploadRequest(request));
    }

    @Test
    void validateUploadRequest_shouldFail_whenInvalidCodePresent() {
        CkycUploadRequest request = validUploadRequest();
        request.getImageDetails().get(0).setImageCode("10");

        assertThrows(CkycValidationException.class, () -> imageValidationUtil.validateUploadRequest(request));
    }

    private CkycUploadRequest validUploadRequest() {
        CkycUploadRequest request = new CkycUploadRequest();
        CkycUploadRequest.IdentityDetail identity = new CkycUploadRequest.IdentityDetail();
        identity.setIdentityType("PAN");
        identity.setIdentityNumber("ABCDE1234F");
        request.setIdentityDetails(List.of(identity));

        CkycUploadRequest.AddressDetail address = new CkycUploadRequest.AddressDetail();
        address.setAddressType("CURRENT");
        address.setLine1("Line1");
        address.setCity("Pune");
        address.setState("MH");
        address.setPinCode("411001");
        address.setCountry("IN");
        request.setAddressDetails(List.of(address));

        CkycUploadRequest.ImageDetail photo = new CkycUploadRequest.ImageDetail();
        photo.setImageCode("70");
        photo.setImageData(base64("photo"));
        photo.setImageFormat("JPG");

        CkycUploadRequest.ImageDetail signature = new CkycUploadRequest.ImageDetail();
        signature.setImageCode("71");
        signature.setImageData(base64("signature"));
        signature.setImageFormat("JPG");

        request.setImageDetails(List.of(photo, signature));
        return request;
    }

    private String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
