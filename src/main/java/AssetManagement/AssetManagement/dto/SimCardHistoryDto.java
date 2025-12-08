// package AssetManagement.AssetManagement.dto;
package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimCardHistoryDto {
    private Long id;
    private String eventType;
    private String details;
    private String performedBy;
    private LocalDateTime performedAt;
}
