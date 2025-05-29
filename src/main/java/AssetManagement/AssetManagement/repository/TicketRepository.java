package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.enums.TicketCategory;
import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    List<Ticket> findByEmployee(User user);

    List<Ticket> findByAssignee(User senderUser);

    List<Ticket> findByEmployeeAndStatus(User employee, TicketStatus status);

    int countByAssignee(User assignee);

    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Optional<Ticket> findById(Long id); // Built-in method

    List<Ticket> findByTitleContainingIgnoreCaseOrCategoryOrStatusOrEmployeeOrAssigneeOrAsset_AssetTagOrLocation(
            String title, TicketCategory category, TicketStatus status, User employee, User assignee, String assetTag, Location location);

    List<Ticket> findByAssigneeAndStatus(User assignee, TicketStatus status);

    Long countByStatus(TicketStatus status);

    @Query("SELECT t FROM Ticket t WHERE t.status = 'OPEN' AND t.createdAt <= :sevenDaysAgo")
    List<Ticket> findOpenTicketsOlderThan(@Param("sevenDaysAgo") LocalDateTime sevenDaysAgo);

}

