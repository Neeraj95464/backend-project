package AssetManagement.AssetManagement.dto;

import lombok.Data;

@Data
public class TicketCcEmailUpdateRequest {
    private String email;
    private boolean add; // true = add, false = remove
}
