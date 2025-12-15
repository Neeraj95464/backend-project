package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.enums.OnboardingStatus;
import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingRowResult {
    private int rowNumber;
    private String employeeId;
    private OnboardingStatus status;
    private String message;
    private List<String> createdTickets; // ticket numbers
    private String createdUserId; // if user created
}