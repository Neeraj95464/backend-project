package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.TicketCategory;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import AssetManagement.AssetManagement.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {
    private Long id;
    private String title;
    private String description;
    private TicketCategory category;
    private TicketStatus status;
    private UserDTO employee;  // Employee ID
    private String createdBy; // Username of the creator
    private UserDTO assignee; // Username of the assigned person
    private String assetTag; // ✅ Asset ID for tracking
    private String assetName; // ✅ Asset Name for display
    private String locationName;
    private Long location;
    private TicketDepartment ticketDepartment;
    private List<String> ccEmails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
//    private LocalDateTime respondedAt;
    private List<TicketMessageDTO> messages; // Messages for progress tracking


    private LocalDateTime firstRespondedAt;
    private LocalDateTime lastUpdated;
    private LocalDateTime dueDate;
    private LocalDateTime closedAt;
    private String attachmentPath;

}
