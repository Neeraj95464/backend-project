package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketFeedbackRepository extends JpaRepository<TicketFeedback, Long> {

    Optional<TicketFeedback> findByTicket(Ticket ticket);

}
