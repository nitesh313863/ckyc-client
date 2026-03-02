package com.example.ckyc.serviceImpl;

import com.example.ckyc.dto.CkycUpdateRequestDto;
import com.example.ckyc.util.ImageMapperUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateXmlBuilderTest {

    private final UpdateXmlBuilder updateXmlBuilder = new UpdateXmlBuilder();

    @Test
    void buildPidData_shouldIncludeOnlyProvidedSections_forPartialUpdate() {
        CkycUpdateRequestDto request = new CkycUpdateRequestDto();
        request.setCkycNo("12345678901234");
        request.setUpdateType("PARTIAL");

        CkycUpdateRequestDto.AddressDetails address = new CkycUpdateRequestDto.AddressDetails();
        address.setLine1("Line1");
        address.setCity("Pune");
        address.setState("MH");
        request.setAddressDetails(address);

        String pidData = updateXmlBuilder.buildPidData(request, "01-01-2026 10:10:10", List.of());

        assertTrue(pidData.contains("<PID_DATA>"));
        assertTrue(pidData.contains("<CKYC_NO>12345678901234</CKYC_NO>"));
        assertTrue(pidData.contains("<UPDATE_TYPE>PARTIAL</UPDATE_TYPE>"));
        assertTrue(pidData.contains("<ADDRESS_DETAILS>"));
        assertFalse(pidData.contains("<PERSONAL_DETAILS>"));
        assertFalse(pidData.contains("<IMAGE_DETAILS>"));
        assertFalse(pidData.contains("<UPDATED_FIELDS>"));
    }

    @Test
    void buildPidData_shouldIncludeIdentityAndImageSections_whenProvided() {
        CkycUpdateRequestDto request = new CkycUpdateRequestDto();
        request.setCkycNo("12345678901234");
        request.setUpdateType("FULL");

        CkycUpdateRequestDto.IdentityDetails identity = new CkycUpdateRequestDto.IdentityDetails();
        identity.setIdType("PAN");
        identity.setIdNumber("ABCDE1234F");
        request.setIdentityList(List.of(identity));

        List<ImageMapperUtil.NormalizedImage> images = List.of(
                new ImageMapperUtil.NormalizedImage("71", "QUJD", "PNG")
        );

        String pidData = updateXmlBuilder.buildPidData(request, "01-01-2026 10:10:10", images);

        assertTrue(pidData.contains("<IDENTITY_DETAILS>"));
        assertTrue(pidData.contains("<IDENTITY_TYPE>PAN</IDENTITY_TYPE>"));
        assertTrue(pidData.contains("<IMAGE_DETAILS>"));
        assertTrue(pidData.contains("<IMAGE_CODE>71</IMAGE_CODE>"));
    }
}
