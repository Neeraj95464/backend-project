package AssetManagement.AssetManagement.dto;

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
    private String employee;  // Employee ID
    private String createdBy; // Username of the creator
    private String assignee; // Username of the assigned person
    private String assetTag; // ✅ Asset ID for tracking
    private String assetName; // ✅ Asset Name for display
    private String locationName;
    private Long location;
    private List<String> ccEmails;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TicketMessageDTO> messages; // Messages for progress tracking

}
