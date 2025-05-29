package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.enums.TicketDepartment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationAssignmentDTO {
    private Long locationId;
    private String locationName;
    private TicketDepartment ticketDepartment;
    private String executiveEmployeeId;
    private String managerEmployeeId;
}
