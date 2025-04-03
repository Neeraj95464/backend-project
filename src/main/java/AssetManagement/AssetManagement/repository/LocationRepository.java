package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findBySiteId(Long siteId);
    List<Location> findByNameContainingIgnoreCase(String name);
    Optional<Location> findById(Long id);
}
