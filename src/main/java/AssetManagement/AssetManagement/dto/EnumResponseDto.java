package AssetManagement.AssetManagement.dto;

import java.util.List;

public class EnumResponseDto {
    private List<String> departments;
    private List<String> assetTypes;

    public EnumResponseDto(List<String> departments, List<String> assetTypes) {
        this.departments = departments;
        this.assetTypes = assetTypes;
    }

    public List<String> getDepartments() {
        return departments;
    }

    public List<String> getAssetTypes() {
        return assetTypes;
    }
}
