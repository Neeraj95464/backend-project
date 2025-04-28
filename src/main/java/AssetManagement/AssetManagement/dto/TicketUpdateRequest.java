package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.enums.TicketStatus;
import lombok.Data;

import java.util.List;

@Data
public class TicketUpdateRequest {
    private TicketStatus status;
    private List<String> ccEmails;
    private Long locationId;
}

