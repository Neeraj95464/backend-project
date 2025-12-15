package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.SimCard;
import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site,Long> {

    Optional<Site> findByName(String name);


}
