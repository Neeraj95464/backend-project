package AssetManagement.AssetManagement.mapper;

import AssetManagement.AssetManagement.dto.TicketDTO;
import AssetManagement.AssetManagement.dto.TicketMessageDTO;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketMessage;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TicketMapper {

    public TicketDTO toDTO(Ticket ticket) {
        return new TicketDTO(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getStatus(),
                ticket.getEmployee() != null ? ticket.getEmployee().getUsername() : null, // Convert User → String
                ticket.getCreatedBy(),
                ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : null, // Convert User → String
                ticket.getAsset() != null ? ticket.getAsset().getAssetTag() : null, // Convert Asset → String
                ticket.getAsset() != null ? ticket.getAsset().getName() : null, // Convert Asset → String
                ticket.getLocation() != null ? ticket.getLocation().getName() : null,
                ticket.getLocation() != null ? ticket.getLocation().getId() : null,// Convert Location → ID
                ticket.getTicketDepartment(),
                ticket.getCcEmails(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                mapMessagesToDTO(ticket.getMessages()) // Convert List<TicketMessage> → List<TicketMessageDTO>
        );
    }

//    public Ticket toEntity(TicketDTO dto) {
//        Ticket ticket = new Ticket();
//        ticket.setId(dto.getId());
//        ticket.setTitle(dto.getTitle());
//        ticket.setDescription(dto.getDescription());
//        ticket.setCategory(dto.getCategory());
//        ticket.setStatus(dto.getStatus());
//        ticket.setCreatedBy(dto.getCreatedBy());
//        ticket.setCreatedAt(dto.getCreatedAt());
//        ticket.setUpdatedAt(dto.getUpdatedAt());
//
//        // Messages Mapping
//        ticket.setMessages(mapMessagesToEntity(dto.getMessages()));
//
//        return ticket;
//    }

    private List<TicketMessageDTO> mapMessagesToDTO(List<TicketMessage> messages) {
        if (messages == null) return null;
        return messages.stream()
                .map(msg -> new TicketMessageDTO(msg.getId(), msg.getMessage(), msg.getSender().getUsername(), msg.getSentAt()))
                .collect(Collectors.toList());
    }

//    private List<TicketMessage> mapMessagesToEntity(List<TicketMessageDTO> messageDTOs) {
//        if (messageDTOs == null) return null;
//        return messageDTOs.stream()
//                .map(dto -> new TicketMessage(dto.getId(),dto.getTicket(),dto.getSender() ,dto.getMessage() ,dto.getSentAt() ))
//                .collect(Collectors.toList());
//    }
}


