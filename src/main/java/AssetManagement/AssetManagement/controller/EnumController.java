package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.enums.Department;
import AssetManagement.AssetManagement.enums.AssetType;
import AssetManagement.AssetManagement.dto.EnumResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/enum/")
public class EnumController {

    @GetMapping("all")
    public EnumResponseDto getAllEnums() {
        List<String> departments = Arrays.stream(Department.values())
                .map(Enum::name)
                .toList();

        List<String> assetTypes = Arrays.stream(AssetType.values())
                .map(Enum::name)
                .toList();

        return new EnumResponseDto(departments, assetTypes);
    }
}
