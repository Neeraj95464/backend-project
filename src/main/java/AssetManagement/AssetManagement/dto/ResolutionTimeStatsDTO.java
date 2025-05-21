package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ResolutionTimeStatsDTO {
    private double avgResolutionTimeInDays;
    private double minResolutionTimeInDays;
    private double maxResolutionTimeInDays;

    public ResolutionTimeStatsDTO(long avgMinutes, long minMinutes, long maxMinutes) {
        this.avgResolutionTimeInDays = avgMinutes / 1440.0; // 1440 minutes = 1 day
        this.minResolutionTimeInDays = minMinutes / 1440.0;
        this.maxResolutionTimeInDays = maxMinutes / 1440.0;
    }

    // Getters and setters (or use Lombok if preferred)
}


