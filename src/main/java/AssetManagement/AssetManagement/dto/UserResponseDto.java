package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.enums.Department;
import lombok.Data;

@Data
public class UserResponseDto {
    private Long id;
    private String username;
    private String employeeId;
    private String role;
    private String phoneNumber;
    private String email;
    private String personalEmail;
    private String designation;
    private Department department;
    private String siteName;
    private String locationName;
    private String aadharNumber;
    private String panNumber;
    private String note;
    private String createdBy;
    private String lastUpdatedBy;
}

