package AssetManagement.AssetManagement.mapper;

import AssetManagement.AssetManagement.dto.TicketDTO;
import AssetManagement.AssetManagement.dto.TicketMessageDTO;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketMessage;
import AssetManagement.AssetManagement.enums.TicketStatus;
import AssetManagement.AssetManagement.repository.TicketMessageRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TicketMapper {
    private final TicketMessageRepository ticketMessageRepository;

    public TicketMapper(TicketMessageRepository ticketMessageRepository) {
        this.ticketMessageRepository = ticketMessageRepository;
    }

//    public TicketDTO toDTO(Ticket ticket) {
//        return new TicketDTO(
//                ticket.getId(),
//                ticket.getTitle(),
//                ticket.getDescription(),
//                ticket.getCategory(),
//                ticket.getStatus(),
//                ticket.getEmployee() != null ? ticket.getEmployee().getUsername() : null, // Convert User → String
//                ticket.getCreatedBy(),
//                ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : null, // Convert User → String
//                ticket.getAsset() != null ? ticket.getAsset().getAssetTag() : null, // Convert Asset → String
//                ticket.getAsset() != null ? ticket.getAsset().getName() : null, // Convert Asset → String
//                ticket.getLocation() != null ? ticket.getLocation().getName() : null,
//                ticket.getLocation() != null ? ticket.getLocation().getId() : null,// Convert Location → ID
//                ticket.getTicketDepartment(),
//                ticket.getCcEmails(),
//                ticket.getCreatedAt(),
//                ticket.getUpdatedAt(),
//                mapMessagesToDTO(ticket.getMessages()) // Convert List<TicketMessage> → List<TicketMessageDTO>
//        );
//    }


    public TicketDTO toDTO(Ticket ticket) {
        List<TicketMessage> messageEntities = ticketMessageRepository.findByTicket(ticket);

        List<TicketMessageDTO> messages = messageEntities
                .stream()
                .map(this::convertMessageToDTO)
                .collect(Collectors.toList());

        // 1. First Responded At — first message by someone other than ticket creator
        LocalDateTime firstRespondedAt = messageEntities.stream()
                .filter(msg -> msg.getSender() != null
                        && ticket.getEmployee() != null
                        && !msg.getSender().getId().equals(ticket.getEmployee().getId()))
                .map(TicketMessage::getSentAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        // 2. Last Updated — latest message or ticket updatedAt
        LocalDateTime lastUpdated = Stream.concat(
                        messageEntities.stream().map(TicketMessage::getSentAt),
                        Stream.of(ticket.getUpdatedAt())
                ).filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(ticket.getUpdatedAt());

        // 3. Due Date — example: 3 business days after ticket creation
        LocalDateTime dueDate = ticket.getCreatedAt() != null
                ? ticket.getCreatedAt().plusDays(3)
                : null;

        // 4. Closed At — only if ticket is closed
        LocalDateTime closedAt = (ticket.getStatus() == TicketStatus.CLOSED)
                ? ticket.getUpdatedAt()
                : null;

        // Build DTO
        TicketDTO dto = new TicketDTO(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getStatus(),
                ticket.getEmployee() != null ? ticket.getEmployee().getUsername() : null,
                ticket.getCreatedBy(),
                ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : null,
                ticket.getAsset() != null ? ticket.getAsset().getAssetTag() : null,
                ticket.getAsset() != null ? ticket.getAsset().getName() : "Other",
                ticket.getLocation() != null ? ticket.getLocation().getName() : null,
                ticket.getLocation() != null ? ticket.getLocation().getId() : null,
                ticket.getTicketDepartment(),
                ticket.getCcEmails(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                messages,
                firstRespondedAt,
                lastUpdated,
                dueDate,
                closedAt
        );

        return dto;
    }

    public TicketMessageDTO convertMessageToDTO(TicketMessage ticketMessage) {
        return new TicketMessageDTO(
                ticketMessage.getId(),
                ticketMessage.getMessage(),
                ticketMessage.getSender().getUsername(), // ✅ Convert User to username
                ticketMessage.getSentAt()
        );
    }

    private List<TicketMessageDTO> mapMessagesToDTO(List<TicketMessage> messages) {
        if (messages == null) return null;
        return messages.stream()
                .map(msg -> new TicketMessageDTO(msg.getId(), msg.getMessage(), msg.getSender().getUsername(), msg.getSentAt()))
                .collect(Collectors.toList());
    }

}


