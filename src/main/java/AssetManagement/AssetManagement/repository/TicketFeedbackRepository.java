package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.dto.AssigneeFeedbackDTO;
import AssetManagement.AssetManagement.dto.AssigneeFeedbackStatsDTO;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketFeedback;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
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
//      AND (:department IS NULL OR t.ticketDepartment = :department)
//    GROUP BY a.id, a.username
//""")
//    List<AssigneeFeedbackDTO> getFeedbackGroupedByAssignee(@Param("department") TicketDepartment department);

    @Query("""
SELECT new AssetManagement.AssetManagement.dto.AssigneeFeedbackStatsDTO(
  a.id,
  a.username,
  AVG(CASE WHEN tf.id IS NOT NULL THEN tf.rating ELSE null END),
  COUNT(tf.id),
  SUM(CASE WHEN t.status = 'CLOSED' THEN 1 ELSE 0 END),
  SUM(CASE WHEN t.status = 'CLOSED' AND tf.id IS NOT NULL THEN 1 ELSE 0 END)
)
FROM Ticket t
JOIN t.assignee a
LEFT JOIN TicketFeedback tf ON tf.ticket = t
WHERE a IS NOT NULL
  AND (
    (:isHrUser = true AND t.ticketDepartment = 'HR') OR
    (:isHrUser = false AND t.ticketDepartment <> 'HR')
  )
GROUP BY a.id, a.username
""")
    List<AssigneeFeedbackStatsDTO> getFeedbackGroupedByAssigneeStats(@Param("isHrUser") boolean isHrUser);



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

    // For HR users: fetch feedbacks where ticket's department is HR
    Page<TicketFeedback> findByTicket_TicketDepartmentAndTicket_Assignee_EmployeeIdContainingIgnoreCase(
            TicketDepartment department, String employeeId, Pageable pageable);

    // For non-HR users: fetch feedbacks where ticket's department is NOT HR
    Page<TicketFeedback> findByTicket_TicketDepartmentNotAndTicket_Assignee_EmployeeIdContainingIgnoreCase(
            TicketDepartment excludedDepartment, String employeeId, Pageable pageable);

    @Query("""
SELECT f FROM TicketFeedback f
WHERE (
    (:isHrUser = true AND f.ticket.ticketDepartment = 'HR') OR
    (:isHrUser = false AND f.ticket.ticketDepartment <> 'HR')
)
AND (:employeeId IS NULL OR f.ticket.assignee.employeeId = :employeeId)
""")
    Page<TicketFeedback> findFilteredFeedbacks(
            @Param("isHrUser") boolean isHrUser,
            @Param("employeeId") String employeeId,
            Pageable pageable
    );

}
