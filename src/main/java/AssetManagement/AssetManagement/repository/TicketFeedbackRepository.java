package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.dto.AssigneeFeedbackDTO;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketFeedback;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketFeedbackRepository extends JpaRepository<TicketFeedback, Long> {

    Optional<TicketFeedback> findByTicket(Ticket ticket);

//    @Query("""
//    SELECT
//        a.id AS assigneeId,
//        a.username AS assigneeName,
//        AVG(tf.rating) AS averageRating,
//        COUNT(tf.id) AS totalFeedbacks
//    FROM TicketFeedback tf
//    JOIN tf.ticket t
//    JOIN t.assignee a
//    WHERE a IS NOT NULL
//    GROUP BY a.id, a.username
//""")
//    List<AssigneeFeedbackDTO> getFeedbackGroupedByAssignee();

    @Query("""
    SELECT 
        a.id AS assigneeId,
        a.username AS assigneeName,
        AVG(tf.rating) AS averageRating,
        COUNT(tf.id) AS totalFeedbacks
    FROM TicketFeedback tf
    JOIN tf.ticket t
    JOIN t.assignee a
    WHERE a IS NOT NULL
      AND (:department IS NULL OR t.ticketDepartment = :department)
    GROUP BY a.id, a.username
""")
    List<AssigneeFeedbackDTO> getFeedbackGroupedByAssignee(@Param("department") TicketDepartment department);


    // TicketFeedbackRepository.java
    @Query("""
    SELECT f FROM TicketFeedback f 
    WHERE (:department IS NULL OR f.ticket.ticketDepartment = :department)
      AND (:employeeId IS NULL OR f.ticket.assignee.employeeId = :employeeId)
""")
    Page<TicketFeedback> findByDepartmentAndAssignee(
            @Param("department") TicketDepartment department,
            @Param("employeeId") String employeeId,
            Pageable pageable
    );

}
