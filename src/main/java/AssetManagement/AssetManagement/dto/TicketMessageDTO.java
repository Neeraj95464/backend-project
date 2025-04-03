package AssetManagement.AssetManagement.dto;


import AssetManagement.AssetManagement.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketMessageDTO {
    private Long id;
//    private Long ticket;
    private String message;
    private String sender; // ✅ Use String for username instead of User object
    private LocalDateTime sentAt;
}


