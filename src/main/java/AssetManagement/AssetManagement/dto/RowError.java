package AssetManagement.AssetManagement.dto;

public record RowError(
        int rowNumber,      // 1-based Excel row index (excluding header)
        String message
) {}
