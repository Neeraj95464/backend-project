package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TicketStatusTimeSeriesDTO {
    private String date; // "yyyy-MM-dd"
    private Map<TicketStatus, Long> statusCount;
}

