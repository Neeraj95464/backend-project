package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.Department;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckOutDTO {
    private LocalDateTime checkOutDate = LocalDateTime.now(); // Defaults to current timestamp
    private User assignedTo;
    private Site site;
    private Location location;
    private Department department;
    private String checkOutNote;
}
