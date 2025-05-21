package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ResolutionTimeStatsDTO {
    private long avgResolutionTime; // in hours or minutes
    private long minResolutionTime;
    private long maxResolutionTime;
}

