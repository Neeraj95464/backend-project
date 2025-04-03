package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.AssetDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssetDocumentRepository extends JpaRepository<AssetDocument, Long> {
    List<AssetDocument> findByAsset(Asset asset);
}

