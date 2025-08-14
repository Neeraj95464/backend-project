package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResolutionStats {
    private Double avg;
    private Double min;
    private Double max;
    private Integer ticketClosed;
    private Integer ticketOpened;
    // constructor, getters
}
