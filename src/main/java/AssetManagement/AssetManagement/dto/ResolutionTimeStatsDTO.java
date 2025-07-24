package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class ResolutionTimeStatsDTO {
    private ResolutionStats overall;
    private Map<String, ResolutionStats> weekly;
    private Map<String, ResolutionStats> monthly;
    // constructor, getters
}




