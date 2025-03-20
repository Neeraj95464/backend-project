package AssetManagement.AssetManagement.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetHistoryDTO {
    private String changedAttribute;  // Example: "status", "location"
    private String oldValue;      // Example: "Checked Out"
    private String newValue;      // Example: "Available"
    private String modifiedBy;    // Example: "admin"
    private LocalDateTime modifiedAt;

}
