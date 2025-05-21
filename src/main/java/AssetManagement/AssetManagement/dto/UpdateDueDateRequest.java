package AssetManagement.AssetManagement.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateDueDateRequest {
    private LocalDateTime dueDate;
}
