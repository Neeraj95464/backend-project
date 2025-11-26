package AssetManagement.AssetManagement.dto;

import java.util.List;

public record AssetImportResult(
        int successCount,
        int failureCount,
        List<RowError> errors
) {}
