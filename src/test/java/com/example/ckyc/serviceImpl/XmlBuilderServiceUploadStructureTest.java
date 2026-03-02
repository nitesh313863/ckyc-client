package com.example.ckyc.serviceImpl;

import com.example.ckyc.dto.CkycUploadRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlBuilderServiceUploadStructureTest {

    private final XmlBuilderService xmlBuilderService = new XmlBuilderService();

    @Test
    void buildUploadPidData_shouldGenerateCkycCompliantIdentityAndImageTags() {
        CkycUploadRequest request = new CkycUploadRequest();
        CkycUploadRequest.PersonalDetails personal = new CkycUploadRequest.PersonalDetails();
        personal.setFirstName("A");
        personal.setLastName("B");
        personal.setDob("01-01-1990");
        personal.setGender("M");
        request.setPersonalDetails(personal);

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

        CkycUploadRequest.ImageDetail image = new CkycUploadRequest.ImageDetail();
        image.setImageCode("70");
        image.setImageData("QUJD");
        image.setImageFormat("PNG");
        request.setImageDetails(List.of(image));

        String xml = xmlBuilderService.buildUploadPidData(request, "01-01-2026 10:10:10");

        assertTrue(xml.contains("<IDENTITY_DETAILS>"));
        assertTrue(xml.contains("<IDENTITY>"));
        assertFalse(xml.contains("<IDENTITY_DETAIL>"));
        assertTrue(xml.contains("<IMAGE_DETAILS>"));
        assertTrue(xml.contains("<IMAGE>"));
        assertFalse(xml.contains("<IMAGE_DETAIL>"));
        assertFalse(xml.contains("<RELATED_PERSONS></RELATED_PERSONS>"));
    }
}
