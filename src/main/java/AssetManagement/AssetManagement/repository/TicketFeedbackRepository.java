package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.dto.AssigneeFeedbackDTO;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TicketFeedbackRepository extends JpaRepository<TicketFeedback, Long> {

    Optional<TicketFeedback> findByTicket(Ticket ticket);

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
    GROUP BY a.id, a.username
""")
    List<AssigneeFeedbackDTO> getFeedbackGroupedByAssignee();

}
