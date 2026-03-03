package com.example.ckyc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CkycUploadRequest {

    @Valid
    private PersonalDetails personalDetails = new PersonalDetails();

    @Valid
    @NotEmpty(message = "At least one identity detail is required")
    private List<IdentityDetail> identityDetails = new ArrayList<>();

    @Valid
    @NotEmpty(message = "At least one address detail is required")
    private List<AddressDetail> addressDetails = new ArrayList<>();

    @Valid
    private List<ImageDetail> imageDetails = new ArrayList<>();

    @Valid
    private List<RelatedPerson> relatedPersons = new ArrayList<>();

    @Data
    public static class PersonalDetails {
        @NotBlank(message = "firstName is mandatory")
        private String firstName;
        private String middleName;
        @NotBlank(message = "lastName is mandatory")
        private String lastName;
        @NotBlank(message = "dob is mandatory")
        @Pattern(regexp = "^\\d{2}-\\d{2}-\\d{4}$", message = "dob must be in dd-MM-yyyy format")
        private String dob;
        @NotBlank(message = "gender is mandatory")
        private String gender;
        @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]$", message = "pan must be valid format")
        private String pan;
        @Pattern(regexp = "^\\d{10}$", message = "mobile must be 10 digits")
        private String mobile;
        @Email(message = "email must be valid")
        private String email;
    }

    @Data
    public static class IdentityDetail {
        @NotBlank(message = "identityType is mandatory")
        private String identityType;
        @NotBlank(message = "identityNumber is mandatory")
        private String identityNumber;
        private String expiryDate;
    }

    @Data
    public static class AddressDetail {
        @NotBlank(message = "addressType is mandatory")
        private String addressType;
        @NotBlank(message = "line1 is mandatory")
        private String line1;
        private String line2;
        @NotBlank(message = "city is mandatory")
        private String city;
        @NotBlank(message = "state is mandatory")
        private String state;
        @NotBlank(message = "pinCode is mandatory")
        private String pinCode;
        @NotBlank(message = "country is mandatory")
        private String country;
    }

    @Data
    public static class ImageDetail {
        @NotBlank(message = "imageCode is mandatory")
        @Pattern(
                regexp = "^(01|02|03|04|05|06|07|70|71)$",
                message = "imageCode must be one of: 01,02,03,04,05,06,07,70,71"
        )
         private String imageCode;
        private String imageType;
        @NotBlank(message = "imageData is mandatory")
        private String imageData;
        @Pattern(regexp = "^(?i)(JPG|JPEG|TIF|TIFF|PDF)$", message = "imageFormat must be JPG, JPEG, TIF, TIFF or PDF")
        private String imageFormat;
        private String sequenceNo;
    }

    @Data
    public static class RelatedPerson {
        @NotBlank(message = "relationType is mandatory")
        private String relationType;
        @NotBlank(message = "name is mandatory")
        private String name;
        @Pattern(regexp = "^\\d{2}-\\d{2}-\\d{4}$", message = "dob must be in dd-MM-yyyy format")
        private String dob;
    }
}
