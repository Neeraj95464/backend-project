// package AssetManagement.AssetManagement.dto;
package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.enums.SimProvider;
import AssetManagement.AssetManagement.enums.SimStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimCardRequestDto {
    private String phoneNumber;
    private String iccid;
    private String imsi;
    private SimProvider provider;
    private SimStatus status;
    private Long assignedUserId; // optional
    private LocalDate activatedAt;
    private LocalDate purchaseDate;
    private String purchaseFrom;
    private BigDecimal cost;
    private String locationName;
    private String siteName;
    private String note;
}
