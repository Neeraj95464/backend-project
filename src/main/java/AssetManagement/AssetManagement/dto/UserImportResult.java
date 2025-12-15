package AssetManagement.AssetManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserImportResult {
    private int created;
    private int skipped;
    private List<String> createdUsers;
    private List<RowError> errors;
}

