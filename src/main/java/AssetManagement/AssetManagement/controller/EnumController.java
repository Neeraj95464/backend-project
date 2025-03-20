package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.enums.Department;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/enum/")
public class EnumController {

    @GetMapping("departments")
    public List<String> getDepartments() {
        return Arrays.stream(Department.values())
                .map(Enum::name)
                .toList();
    }
}
