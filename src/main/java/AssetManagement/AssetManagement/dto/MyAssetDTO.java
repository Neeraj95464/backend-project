package AssetManagement.AssetManagement.dto;
// package AssetManagement.AssetManagement.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MyAssetDTO {

    // e.g. LAP-001, DESK-123, or SIM-XXXX
    private String assetTag;

    // e.g. LAPTOP, DESKTOP, CUG_SIM, MONITOR, ACCESS_CARD, etc.
    private String assetType;

    // Human readable name/label (Laptop, Desktop PC, CUG SIM, etc.)
    private String name;

    // For hardware assets: serial number; for SIM: phone number
    private String identifier;

    // For SIM: provider like JIO, AIRTEL; for others you can reuse brand/model
    private String providerOrBrand;

    private String locationName;
    private String siteName;

    private String status;

    // Optional extra info for UI tooltip or small note
    private String note;
}

