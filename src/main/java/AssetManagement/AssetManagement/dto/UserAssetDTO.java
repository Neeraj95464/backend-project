package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAssetDTO {
    private String assetTag;
    private String name;

    private String brand;
    private String model;
    private String department;
    private String assetType;
    private String createdBy;
    private LocalDateTime createdAt;
    private String description;
    private String serialNumber;
    private LocalDate purchaseDate;
    private String status;
    private String locationName;
    private String siteName;
    private String assignedUserName;

    // Additional fields for notes
    private String statusNote;
    private LocalDate reservationStartDate;
    private LocalDate reservationEndDate;
}
