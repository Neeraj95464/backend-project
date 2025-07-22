package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildAssetDTO {

    private String name;
    private String warranty;
    private String purchaseFrom;
    private String childAssetNote;
}
