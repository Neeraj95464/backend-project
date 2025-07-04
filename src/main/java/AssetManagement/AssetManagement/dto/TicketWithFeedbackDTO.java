// TicketWithFeedbackDTO.java
package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.enums.TicketCategory;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import AssetManagement.AssetManagement.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TicketWithFeedbackDTO {
    private Long ticketId;
    private String title;
    private String description;
    private TicketCategory category;
    private TicketStatus status;
    private TicketDepartment department;
    private String createdBy;
    private String assigneeName;
    private LocalDateTime createdAt;
    private int rating;
    private String feedbackMessage;
    private LocalDateTime submittedAt;
}
