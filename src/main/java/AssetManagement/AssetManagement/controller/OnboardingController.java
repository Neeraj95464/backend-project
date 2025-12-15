package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.OnboardingResultDto;
import AssetManagement.AssetManagement.service.EmployeeOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final EmployeeOnboardingService onboardingService;

    @PostMapping("/employees/bulk")
    public ResponseEntity<OnboardingResultDto> bulkOnboardEmployees(
            @RequestParam("file") MultipartFile file) {

        OnboardingResultDto result = onboardingService.processOnboardingExcel(file);
        return ResponseEntity.ok(result);
    }
}
