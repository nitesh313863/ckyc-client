package com.example.ckyc.util;

import com.example.ckyc.dto.CkycDownloadResponseDto;
import com.example.ckyc.dto.CkycSearchResponseDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CkycResponseMapper {

    private CkycResponseMapper() {
    }

    public static CkycSearchResponseDto toSearchResponse(CkycPidDataParser.ParsedPidData parsedPidData) {
        if (parsedPidData == null) {
            return null;
        }
        Map<String, Object> pidData = extractPidDataMap(parsedPidData);
        if (pidData.isEmpty()) {
            return null;
        }
        Map<String, Object> searchPid = unwrapSearchPid(pidData);
        Map<String, Object> additional = new LinkedHashMap<>(searchPid);

        String ckycNo = asString(searchPid.get("CKYC_NO"));
        String ckycReferenceId = asString(searchPid.get("CKYC_REFERENCE_ID"));
        String name = asString(searchPid.get("NAME"));
        String fathersName = asString(searchPid.get("FATHERS_NAME"));
        String age = asString(searchPid.get("AGE"));
        String imageType = asString(searchPid.get("IMAGE_TYPE"));
        String photo = asString(searchPid.get("PHOTO"));
        String kycDate = asString(searchPid.get("KYC_DATE"));
        String updatedDate = asString(searchPid.get("UPDATED_DATE"));
        String remarks = asString(searchPid.get("REMARKS"));

        List<CkycSearchResponseDto.SearchIdStatus> idList = mapIdList(searchPid);

        additional.keySet().removeAll(List.of(
                "CKYC_NO",
                "CKYC_REFERENCE_ID",
                "NAME",
                "FATHERS_NAME",
                "AGE",
                "IMAGE_TYPE",
                "PHOTO",
                "KYC_DATE",
                "UPDATED_DATE",
                "REMARKS",
                "ID_LIST",
                "ID-LIST",
                "IDLIST",
                "SearchResponsePID"
        ));

        if (additional.isEmpty()) {
            additional = null;
        }

        return CkycSearchResponseDto.builder()
                .ckycNo(ckycNo)
                .ckycReferenceId(ckycReferenceId)
                .name(name)
                .fathersName(fathersName)
                .age(age)
                .imageType(imageType)
                .photo(photo)
                .kycDate(kycDate)
                .updatedDate(updatedDate)
                .remarks(remarks)
                .idList(idList == null || idList.isEmpty() ? null : idList)
                .additionalFields(additional)
                .build();
    }

    public static CkycDownloadResponseDto toDownloadResponse(CkycPidDataParser.ParsedPidData parsedPidData) {
        if (parsedPidData == null) {
            return null;
        }
        Map<String, Object> pidData = extractPidDataMap(parsedPidData);
        if (pidData.isEmpty()) {
            return null;
        }

        Map<String, Object> recordCountDetails = asMap(pidData.get("RECORD_COUNT_DETAILS"));
        Map<String, Object> personalDetails = asMap(pidData.get("PERSONAL_DETAILS"));
        Map<String, Object> addressDetails = asMap(pidData.get("ADDRESS_DETAILS"));
        List<Map<String, Object>> identityDetails = extractList(pidData, "IDENTITY_DETAILS", "IDENTITY");
        List<Map<String, Object>> relatedPersons = extractList(pidData, "RELATED_PERSONS", "RELATED_PERSON");
        List<Map<String, Object>> images = extractList(pidData, "IMAGE_DETAILS", "IMAGE");

        Map<String, Object> legalEntityDetails = firstMap(
                pidData.get("LEGAL_ENTITY_DETAILS"),
                pidData.get("LEGAL_DETAILS")
        );

        Map<String, Object> otherDetails = new LinkedHashMap<>(pidData);
        otherDetails.keySet().removeAll(List.of(
                "RECORD_COUNT_DETAILS",
                "PERSONAL_DETAILS",
                "ADDRESS_DETAILS",
                "IDENTITY_DETAILS",
                "RELATED_PERSONS",
                "IMAGE_DETAILS",
                "LEGAL_ENTITY_DETAILS",
                "LEGAL_DETAILS"
        ));
        if (otherDetails.isEmpty()) {
            otherDetails = null;
        }

        return CkycDownloadResponseDto.builder()
                .recordCountDetails(recordCountDetails)
                .personalDetails(personalDetails)
                .addressDetails(addressDetails)
                .identityDetails(identityDetails == null || identityDetails.isEmpty() ? null : identityDetails)
                .relatedPersons(relatedPersons == null || relatedPersons.isEmpty() ? null : relatedPersons)
                .images(images == null || images.isEmpty() ? null : images)
                .legalEntityDetails(legalEntityDetails)
                .otherDetails(otherDetails)
                .build();
    }

    public static Map<String, Object> extractPidDataMap(CkycPidDataParser.ParsedPidData parsedPidData) {
        if (parsedPidData == null) {
            return Map.of();
        }
        Map<String, Object> structured = parsedPidData.getStructured();
        if (structured == null || structured.isEmpty()) {
            return Map.of();
        }
        Object pidData = structured.get("PID_DATA");
        if (pidData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) pidData;
            return map;
        }
        return structured;
    }

    private static Map<String, Object> unwrapSearchPid(Map<String, Object> pidData) {
        Object searchResponse = pidData.get("SearchResponsePID");
        if (searchResponse instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) searchResponse;
            return map;
        }
        return pidData;
    }

    private static List<CkycSearchResponseDto.SearchIdStatus> mapIdList(Map<String, Object> searchPid) {
        Object idListObj = firstNonNull(
                searchPid.get("ID_LIST"),
                searchPid.get("ID-LIST"),
                searchPid.get("IDLIST")
        );
        if (idListObj == null) {
            return null;
        }

        List<Map<String, Object>> items = extractListFromContainer(idListObj, "ID");
        if (items == null || items.isEmpty()) {
            return null;
        }
        List<CkycSearchResponseDto.SearchIdStatus> results = new ArrayList<>();
        for (Map<String, Object> item : items) {
            if (item == null) {
                continue;
            }
            String type = asString(item.get("TYPE"));
            String status = asString(item.get("STATUS"));
            results.add(CkycSearchResponseDto.SearchIdStatus.builder()
                    .type(type)
                    .status(status)
                    .build());
        }
        return results;
    }

    private static List<Map<String, Object>> extractList(
            Map<String, Object> pidData,
            String containerKey,
            String itemKey
    ) {
        Object container = pidData.get(containerKey);
        if (container == null) {
            return null;
        }
        return extractListFromContainer(container, itemKey);
    }

    private static List<Map<String, Object>> extractListFromContainer(Object container, String itemKey) {
        if (container instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) container;
            Object items = map.get(itemKey);
            return normalizeList(items);
        }
        return normalizeList(container);
    }

    private static List<Map<String, Object>> normalizeList(Object items) {
        if (items == null) {
            return null;
        }
        List<Map<String, Object>> results = new ArrayList<>();
        if (items instanceof List) {
            for (Object item : (List<?>) items) {
                Map<String, Object> mapped = asMap(item);
                if (mapped != null) {
                    results.add(mapped);
                }
            }
            return results;
        }
        Map<String, Object> mapped = asMap(items);
        if (mapped != null) {
            results.add(mapped);
        }
        return results;
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map;
        }
        return null;
    }

    private static Map<String, Object> firstMap(Object... values) {
        for (Object value : values) {
            Map<String, Object> map = asMap(value);
            if (map != null && !map.isEmpty()) {
                return map;
            }
        }
        return null;
    }

    private static Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
