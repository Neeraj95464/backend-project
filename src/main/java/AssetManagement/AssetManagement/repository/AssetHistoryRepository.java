package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.AssetHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetHistoryRepository extends JpaRepository<AssetHistory, Long> {

    List<AssetHistory> findByAsset_AssetTagOrderByModifiedAtDesc(String assetTag);
}
