package AssetManagement.AssetManagement.util;

import AssetManagement.AssetManagement.dto.ChildAssetDTO;
import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.ChildAsset;

public class ChildAssetConverter {

    public static ChildAsset toEntity(ChildAssetDTO dto, Asset parentAsset) {
        ChildAsset childAsset = new ChildAsset();
        childAsset.setName(dto.getName());
        childAsset.setWarranty(dto.getWarranty());
        childAsset.setPurchaseFrom(dto.getPurchaseFrom());
        childAsset.setChildAssetNote(dto.getChildAssetNote()); // ✅ Add this line
        childAsset.setParentAsset(parentAsset);
        return childAsset;
    }



    public static ChildAssetDTO toDTO(ChildAsset entity) {
        return new ChildAssetDTO(
                entity.getName(),
                entity.getWarranty(),
                entity.getPurchaseFrom(),
                entity.getChildAssetNote()
        );
    }
}
