package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.Department;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CheckOutDTO {
    private LocalDateTime checkOutDate = LocalDateTime.now(); // Defaults to current timestamp
    private User assignedTo;
    private boolean assignedToLocation;
    private Site site;
    private Location location;
    private Department department;
    private String checkOutNote;
}
