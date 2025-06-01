package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class ResolutionTimeStatsDTO {
    private Long avgResolutionTimeInMinutes;
    private Long minResolutionTimeInMinutes;
    private Long maxResolutionTimeInMinutes;

    public ResolutionTimeStatsDTO(Long avg, Long min, Long max) {
        this.avgResolutionTimeInMinutes = avg;
        this.minResolutionTimeInMinutes = min;
        this.maxResolutionTimeInMinutes = max;
    }

    // Getters, setters, etc.
}



