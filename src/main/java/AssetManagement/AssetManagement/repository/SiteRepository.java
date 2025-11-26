package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site,Long> {

    Optional<Site> findByName(String name);
}
