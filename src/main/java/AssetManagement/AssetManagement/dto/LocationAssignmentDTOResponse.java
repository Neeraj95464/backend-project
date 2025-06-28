package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.enums.TicketDepartment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationAssignmentDTOResponse {
    private String location;
    private TicketDepartment department;
    private UserIdNameDTO itExecutive;
    private UserIdNameDTO locationManager;
}
