package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.enums.Department;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckInDTO {
    private String checkInNote;
    private Site site;
    private Location location;
    private LocalDateTime checkInDate = LocalDateTime.now();
    private Department department;

}
