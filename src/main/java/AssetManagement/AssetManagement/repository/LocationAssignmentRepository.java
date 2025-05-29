package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.LocationAssignment;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationAssignmentRepository extends JpaRepository<LocationAssignment, Long> {

    @Query("SELECT la.itExecutive FROM LocationAssignment la WHERE la.location.id = :locationId")
    List<User> findExecutivesByLocation(@Param("locationId") Long locationId);

    @Query("SELECT la.itExecutive FROM LocationAssignment la WHERE la.location.id = :locationId AND la.ticketDepartment = :department")
    List<User> findExecutivesByLocationAndDepartment(@Param("locationId") Long locationId, @Param("department") TicketDepartment department);


    List<LocationAssignment> findByLocation(Location location);

    Optional<LocationAssignment> findByLocationAndTicketDepartment(Location location, TicketDepartment ticketDepartment);

    @Query("SELECT la.locationManager FROM LocationAssignment la WHERE la.location = :location AND la.ticketDepartment = :department")
    User findLocationManagerByLocationAndTicketDepartment(@Param("location") Location location, @Param("department") TicketDepartment department);

}

