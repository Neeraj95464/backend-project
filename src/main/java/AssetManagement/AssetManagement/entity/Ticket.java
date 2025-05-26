package AssetManagement.AssetManagement.entity;

import AssetManagement.AssetManagement.enums.TicketCategory;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import AssetManagement.AssetManagement.enums.TicketStatus;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

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

        @Column(columnDefinition = "TEXT")
        private String description;

        @Enumerated(EnumType.STRING)
        private TicketCategory category; // Enum for categorization

        @Enumerated(EnumType.STRING)
        private TicketStatus status; // Open, In Progress, Resolved, Closed

        private String messageId;
        private String internetMessageId;

        @ManyToOne
        @JoinColumn(name = "employee")
        private User employee; // User who created the ticket

        private String createdBy;

        @ManyToOne
        @JoinColumn(name = "assignee")
        private User assignee; // Who is assigned to resolve the ticket

        @Enumerated(EnumType.STRING)
        @NotNull
        private TicketDepartment ticketDepartment;

        @ManyToOne
        @JoinColumn(name="ticket_location")
        private Location location;

        private LocalDateTime dueDate;

        @ElementCollection
        private List<String> ccEmails; // Store CC'd email addresses

        @ManyToOne
        @JoinColumn(name = "asset_id") // Link to Asset Table
        private Asset asset;

        @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<TicketMessage> messages = new ArrayList<>(); // Messages for communication

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        @Column(name = "attachment_path")
        private String attachmentPath; // File system path to the attachment

}

