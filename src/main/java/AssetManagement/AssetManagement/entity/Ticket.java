package AssetManagement.AssetManagement.entity;

import AssetManagement.AssetManagement.dto.TicketCategory;
import AssetManagement.AssetManagement.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String title;
        private String description;

        @Enumerated(EnumType.STRING)
        private TicketCategory category; // Enum for categorization

        @Enumerated(EnumType.STRING)
        private TicketStatus status; // Open, In Progress, Resolved, Closed

        private String messageId;
        @ManyToOne
        @JoinColumn(name = "employee")
        private User employee; // User who created the ticket

        private String createdBy;

        @ManyToOne
        @JoinColumn(name = "assignee")
        private User assignee; // Who is assigned to resolve the ticket

        @ManyToOne
        @JoinColumn(name="ticket_location")
        private Location location;

        @ElementCollection
        private List<String> ccEmails; // Store CC'd email addresses

        @ManyToOne
        @JoinColumn(name = "asset_id") // Link to Asset Table
        private Asset asset;

        @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<TicketMessage> messages = new ArrayList<>(); // Messages for communication

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

}

