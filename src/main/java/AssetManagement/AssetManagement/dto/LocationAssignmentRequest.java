package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.enums.TicketDepartment;
import lombok.Data;

@Data
public class LocationAssignmentRequest {
    private Long locationId;
    private String executiveEmployeeId;
    private String managerEmployeeId;
    private TicketDepartment ticketDepartment;
}
