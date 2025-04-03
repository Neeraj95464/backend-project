package AssetManagement.AssetManagement.entity;

import AssetManagement.AssetManagement.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender; // Can be either the creator or the assignee

    @Column(columnDefinition = "TEXT", nullable = false) // Use TEXT type for longer messages
    private String message;

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    private TicketStatus statusUpdate; // Stores the new status

    @ManyToOne
    @JoinColumn(name = "status_updated_by")
    private User statusUpdatedBy; // User who updated the status

    @Column(unique = true)
    private String messageId; // ✅ Stores email Message-ID for threading
}
