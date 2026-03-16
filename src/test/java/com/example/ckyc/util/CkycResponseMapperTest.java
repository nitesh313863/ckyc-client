package com.example.ckyc.util;

import com.example.ckyc.dto.CkycDownloadResponseDto;
import com.example.ckyc.dto.CkycSearchResponseDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CkycResponseMapperTest {

    @Test
    void toSearchResponse_shouldMapCoreFields() {
        String pidData = "<PID_DATA>"
                + "<CKYC_NO>12345678901234</CKYC_NO>"
                + "<CKYC_REFERENCE_ID>INABCD12345678</CKYC_REFERENCE_ID>"
                + "<NAME>Alex</NAME>"
                + "<FATHERS_NAME>Raj</FATHERS_NAME>"
                + "<AGE>27</AGE>"
                + "<IMAGE_TYPE>jpg</IMAGE_TYPE>"
                + "<PHOTO>BASE64PHOTO</PHOTO>"
                + "<KYC_DATE>06-09-2016</KYC_DATE>"
                + "<UPDATED_DATE>15-09-2016</UPDATED_DATE>"
                + "<ID_LIST><ID><TYPE>PAN</TYPE><STATUS>ACTIVE</STATUS></ID></ID_LIST>"
                + "<REMARKS>OK</REMARKS>"
                + "</PID_DATA>";

        CkycPidDataParser.ParsedPidData parsed = CkycPidDataParser.parse(pidData);
        CkycSearchResponseDto response = CkycResponseMapper.toSearchResponse(parsed);

        assertNotNull(response);
        assertEquals("12345678901234", response.getCkycNo());
        assertEquals("INABCD12345678", response.getCkycReferenceId());
        assertEquals("Alex", response.getName());
        assertEquals("Raj", response.getFathersName());
        assertEquals("jpg", response.getImageType());
        assertEquals("BASE64PHOTO", response.getPhoto());
        assertEquals("06-09-2016", response.getKycDate());

        List<CkycSearchResponseDto.SearchIdStatus> idList = response.getIdList();
        assertNotNull(idList);
        assertEquals(1, idList.size());
        assertEquals("PAN", idList.get(0).getType());
        assertEquals("ACTIVE", idList.get(0).getStatus());
    }

    @Test
    void toDownloadResponse_shouldMapCoreSections() {
        String pidData = "<PID_DATA>"
                + "<RECORD_COUNT_DETAILS><DOWNLOAD_COUNT>1</DOWNLOAD_COUNT><UPDATE_COUNT>0</UPDATE_COUNT></RECORD_COUNT_DETAILS>"
                + "<PERSONAL_DETAILS><CKYC_NO>12345678901234</CKYC_NO><FNAME>John</FNAME></PERSONAL_DETAILS>"
                + "<IDENTITY_DETAILS><IDENTITY><TYPE>PAN</TYPE><NUMBER>ABCDE1234F</NUMBER></IDENTITY></IDENTITY_DETAILS>"
                + "<ADDRESS_DETAILS><PERM_CITY>Mumbai</PERM_CITY></ADDRESS_DETAILS>"
                + "<RELATED_PERSONS><RELATED_PERSON><NAME>Jane</NAME></RELATED_PERSON></RELATED_PERSONS>"
                + "<IMAGE_DETAILS><IMAGE><IMAGE_TYPE>jpg</IMAGE_TYPE></IMAGE></IMAGE_DETAILS>"
                + "</PID_DATA>";

        CkycPidDataParser.ParsedPidData parsed = CkycPidDataParser.parse(pidData);
        CkycDownloadResponseDto response = CkycResponseMapper.toDownloadResponse(parsed);

        assertNotNull(response);
        assertEquals("1", response.getRecordCountDetails().get("DOWNLOAD_COUNT"));
        assertEquals("John", response.getPersonalDetails().get("FNAME"));

        List<Map<String, Object>> identities = response.getIdentityDetails();
        assertNotNull(identities);
        assertEquals(1, identities.size());
        assertEquals("PAN", identities.get(0).get("TYPE"));

        List<Map<String, Object>> related = response.getRelatedPersons();
        assertNotNull(related);
        assertEquals("Jane", related.get(0).get("NAME"));

        List<Map<String, Object>> images = response.getImages();
        assertNotNull(images);
        assertEquals("jpg", images.get(0).get("IMAGE_TYPE"));
    }
}
