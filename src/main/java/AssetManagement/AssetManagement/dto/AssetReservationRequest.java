package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AssetReservationRequest {
    private LocalDate reservationStartDate;
    private LocalDate reservationEndDate;
    private String statusNote;
    private Long reservedForUserId;  // Instead of User entity
    private Long reserveForSiteId;   // Instead of Site entity
    private Long reserveForLocationId; // Instead of Location entity
}
