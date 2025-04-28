package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    List<TicketMessage> findByTicket(Ticket ticket);

    TicketMessage findTopByTicketOrderBySentAtDesc(Ticket ticket);

    TicketMessage findTopByTicketOrderBySentAtAsc(Ticket ticket);
//    List<TicketMessage> findByTicket(Ticket ticket);

}
