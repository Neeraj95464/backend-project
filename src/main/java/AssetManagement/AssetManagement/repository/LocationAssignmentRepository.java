package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.LocationAssignment;
import AssetManagement.AssetManagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationAssignmentRepository extends JpaRepository<LocationAssignment, Long> {

    @Query("SELECT la.itExecutive FROM LocationAssignment la WHERE la.location.id = :locationId")
    List<User> findExecutivesByLocation(@Param("locationId") Long locationId);

//    List<User> findExecutivesByLocation(@Param("locationId") Long locationId);
}

