package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AssigneeFeedbackStatsDTO {
    private Long assigneeId;
    private String assigneeName;
    private Double averageRating;
    private Long totalFeedbacks;
    private Long closedTickets;
    private Long closedTicketsWithFeedback;

    // Getters and setters (or use Lombok @Data)
}

