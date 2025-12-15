//// EmployeeOnboardingRequestDto.java
//package AssetManagement.AssetManagement.dto;
//
//import lombok.Data;
//import java.util.List;
//
//@Data
//public class EmployeeOnboardingRequestDto {
//    private String employeeId;
//    private String firstName;
//    private String lastName;
//    private String email;
//    private String departmentName;
//    private String siteName;
//    private String locationName;
//    private String role;
//    private List<String> requiredAssets; // e.g., ["Laptop", "SIM", "Monitor"]
//}
//
//
//



package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.enums.Department;
import lombok.Data;

@Data
public class EmployeeOnboardingRequestDto {
    // Employee Details (Columns A-K: 0-10)
    private String employeeId;
    private String username;
    private String professionalEmail;
    private String personalEmail;
    private String phoneNumber;
    private Department department;
    private String siteName;
    private String locationName;
    private String aadharNumber;
    private String panNumber;
    private String note;

    // Asset Requirements (Columns L-N: 11-13)
    private boolean cugSimRequired;      // L - CUG/SIM (Yes/No)
    private boolean laptopDesktopRequired; // M - Laptop/Desktop (Yes/No)
    private boolean emailSetupRequired;   // N - Email Setup (Yes/No)

    // Helpers
    public boolean isCugSimRequired() { return cugSimRequired; }
    public boolean isLaptopDesktopRequired() { return laptopDesktopRequired; }
    public boolean isEmailSetupRequired() { return emailSetupRequired; }
}
