package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByAssignedUser(User user);
    List<Asset> findByNameContainingIgnoreCaseOrSerialNumberContainingIgnoreCase(String name, String serialNumber);
    List<Asset> findByLocationId(Long locationId);
    long countByStatus(AssetStatus status);
    Optional<Asset> findBySerialNumber(String serialNumber);
    List<Asset> findByStatus(AssetStatus status);
    Optional<Asset> findByAssetTag(String assetTag);
    
}

