package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.AssetHistory;
import AssetManagement.AssetManagement.repository.AssetHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AssetHistoryService {
    private final AssetHistoryRepository assetHistoryRepository;

    public AssetHistoryService(AssetHistoryRepository assetHistoryRepository) {
        this.assetHistoryRepository = assetHistoryRepository;
    }

    public void saveAssetHistory(Asset asset, String field, String oldValue, String newValue, String modifiedBy) {
        if (Objects.equals(oldValue, newValue)) return; // ✅ Skip unchanged fields

        AssetHistory history = new AssetHistory();
        history.setAsset(asset);
        history.setChangedAttribute(field);
        history.setOldValue(oldValue != null ? oldValue : "NULL");  // Handle null values
        history.setNewValue(newValue != null ? newValue : "NULL");  // Handle null values
        history.setModifiedBy(modifiedBy);
        history.setModifiedAt(LocalDateTime.now());

        assetHistoryRepository.save(history); // ✅ Save history entry
    }

    public List<AssetHistory> getAssetHistoryByAssetTag(String assetTag) {
        return assetHistoryRepository.findByAsset_AssetTagOrderByModifiedAtDesc(assetTag);
    }
}
