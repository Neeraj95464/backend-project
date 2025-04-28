package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.ChildAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildAssetRepository extends JpaRepository<ChildAsset, Long> {
    // Custom queries can be added here if needed
}
