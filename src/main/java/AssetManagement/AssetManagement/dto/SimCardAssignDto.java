// package AssetManagement.AssetManagement.dto;
package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimCardAssignDto {
    private String employeeId; // assign to this user
    private String performedBy; // who performed assignment
    private String note; // optional
}
