package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.enums.TicketCategory;
import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import AssetManagement.AssetManagement.enums.TicketStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    List<Ticket> findByEmployee(User user);

    List<Ticket> findByAssignee(User senderUser);

    List<Ticket> findByEmployeeAndStatus(User employee, TicketStatus status);

    int countByAssignee(User assignee);

    @NotNull
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    List<Ticket> findByStatus(TicketStatus status);

    @NotNull
    Optional<Ticket> findById(Long id); // Built-in method

    List<Ticket> findByTitleContainingIgnoreCaseOrCategoryOrStatusOrEmployeeOrAssigneeOrAsset_AssetTagOrLocation(
            String title, TicketCategory category, TicketStatus status, User employee, User assignee, String assetTag, Location location);

    List<Ticket> findByAssigneeAndStatus(User assignee, TicketStatus status);

    Long countByStatus(TicketStatus status);

    @Query("SELECT t FROM Ticket t WHERE t.status = 'OPEN' AND t.createdAt <= :sevenDaysAgo")
    List<Ticket> findOpenTicketsOlderThan(@Param("sevenDaysAgo") LocalDateTime sevenDaysAgo);


    @Query("SELECT t FROM Ticket t WHERE t.createdAt BETWEEN :start AND :end")
    List<Ticket> findTicketsCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
    SELECT t FROM Ticket t
    WHERE t.createdAt BETWEEN :start AND :end
      AND (
          (:isHrUser = true AND t.ticketDepartment = 'HR') OR
          (:isHrUser = false AND t.ticketDepartment <> 'HR')
      )
""")
    List<Ticket> findTicketsCreatedBetweenFiltered(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("isHrUser") boolean isHrUser
    );


    Page<Ticket> findByTicketDepartment(TicketDepartment ticketDepartment, Pageable pageable);

    Page<Ticket> findByTicketDepartmentAndStatus(TicketDepartment ticketDepartment, TicketStatus status, Pageable pageable);


    Page<Ticket> findByTicketDepartmentIsNullOrTicketDepartment(TicketDepartment ticketDepartment, PageRequest pageRequest);

    Page<Ticket> findByStatusAndTicketDepartmentIsNullOrTicketDepartment(TicketStatus status, TicketDepartment ticketDepartment, PageRequest pageRequest);

    Page<Ticket> findByAssigneeIsNull(PageRequest pageRequest);

    // Option 1: Using JPA naming convention
    List<Ticket> findByLocation_Site_Id(Long siteId);

    // Option 2: Explicit JPQL
    List<Ticket> findByLocation_Site_IdAndStatus(Long siteId, TicketStatus status);

//    Page<Ticket> findByLocation_Site_IdAndStatusAndCreatedAtBetween(Long siteId, TicketStatus ticketStatus, LocalDateTime startDate, LocalDateTime endDate, Pageable pageRequest);

    @Query("""
    SELECT t FROM Ticket t
    WHERE (:siteId IS NULL OR t.location.site.id = :siteId)
      AND (:department IS NULL OR t.ticketDepartment = :department)
      AND (:status IS NULL OR t.status = :status)
      AND (:startDate IS NULL OR t.createdAt >= :startDate)
      AND (:endDate IS NULL OR t.createdAt <= :endDate)
""")
    Page<Ticket> findByLocation_Site_IdAndStatusAndCreatedAtBetween(
            @Param("siteId") Long siteId,
            @Param("department") TicketDepartment department,
            @Param("status") TicketStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );


}

