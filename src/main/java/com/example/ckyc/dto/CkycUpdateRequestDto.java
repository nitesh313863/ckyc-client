package com.example.ckyc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CkycUpdateRequestDto {

    @NotBlank(message = "ckycNo is mandatory")
    @Pattern(regexp = "^\\d{14}$", message = "ckycNo must be numeric and 14 digits")
    private String ckycNo;

    @NotBlank(message = "updateType is mandatory")
    @Pattern(regexp = "^(FULL|PARTIAL)$", flags = Pattern.Flag.CASE_INSENSITIVE, message = "updateType must be FULL or PARTIAL")
    private String updateType;

    @Valid
    private PersonalDetails personalDetails;

    @Valid
    private AddressDetails addressDetails;

    @Valid
    private List<IdentityDetails> identityList = new ArrayList<>();

    @Valid
    private List<ImageDetails> imageList = new ArrayList<>();

    @Valid
    private List<RelatedPerson> relatedPersons = new ArrayList<>();

    @Data
    public static class PersonalDetails {
        private String firstName;
        private String middleName;
        private String lastName;
        @Pattern(regexp = "^\\d{2}-\\d{2}-\\d{4}$", message = "dob must be in dd-MM-yyyy format")
        private String dob;
        private String gender;
        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "pan must be valid format")
        private String pan;
        @Pattern(regexp = "^\\d{12}$", message = "aadhaar must be 12 digits")
        private String aadhaar;
        @Pattern(regexp = "^\\d{10}$", message = "mobile must be 10 digits")
        private String mobile;
        @Email(message = "email must be valid")
        private String email;
    }

    @Data
    public static class AddressDetails {
        private String addressType;
        private String line1;
        private String line2;
        private String city;
        private String district;
        private String state;
        private String pinCode;
        private String country;
    }

    @Data
    public static class IdentityDetails {
        private String idType;
        private String idNumber;
        private String issueDate;
        private String expiryDate;
    }

    @Data
    public static class ImageDetails {
        @NotBlank(message = "imageCode is mandatory")
        @Pattern(
                regexp = "^(01|02|03|04|05|06|07|70|71)$",
                message = "imageCode must be one of: 01,02,03,04,05,06,07,70,71"
        )
        private String imageCode;
        @NotBlank(message = "imageData is mandatory")
        private String imageData;
        @Pattern(regexp = "^(?i)(JPG|JPEG|TIF|TIFF|PDF)$", message = "imageFormat must be JPG, JPEG, TIF, TIFF or PDF")
        private String imageFormat;
    }

    @Data
    public static class RelatedPerson {
        private String relationType;
        private String name;
        @Pattern(regexp = "^\\d{2}-\\d{2}-\\d{4}$", message = "dob must be in dd-MM-yyyy format")
        private String dob;
    }
}
