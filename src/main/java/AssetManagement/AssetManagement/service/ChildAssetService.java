package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.ChildAssetDTO;
import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.ChildAsset;
import AssetManagement.AssetManagement.repository.AssetRepository;
import AssetManagement.AssetManagement.repository.ChildAssetRepository;
import AssetManagement.AssetManagement.util.ChildAssetConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChildAssetService {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private ChildAssetRepository childAssetRepository;

    // Fetch small child assets by assetTag
    public List<ChildAsset> getSmallChildAssetsByAssetTag(String assetTag) {
        Optional<Asset> asset = assetRepository.findByAssetTag(assetTag);
        if (asset.isPresent()) {
            return asset.get().getSmallChildAssets(); // Get small child assets for the given asset
        } else {
            throw new RuntimeException("Asset not found with assetTag: " + assetTag);
        }
    }

    // Create a small child asset using a DTO and assetTag
    public ChildAssetDTO createSmallChildAsset(String assetTag, ChildAssetDTO dto) {
        Asset asset = assetRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new RuntimeException("Asset not found with assetTag: " + assetTag));

        ChildAsset childAsset = ChildAssetConverter.toEntity(dto, asset);
        ChildAsset saved = childAssetRepository.save(childAsset);
        return ChildAssetConverter.toDTO(saved);
    }
}
