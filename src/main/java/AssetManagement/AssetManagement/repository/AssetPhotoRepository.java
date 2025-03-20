package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.AssetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssetPhotoRepository extends JpaRepository<AssetPhoto, Long> {
    List<AssetPhoto> findByAsset(Asset asset);
}

