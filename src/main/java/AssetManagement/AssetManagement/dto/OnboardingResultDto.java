package AssetManagement.AssetManagement.dto;


import lombok.*;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class OnboardingResultDto {
    private int totalEmployees;
    private int successfulOnboardings;
    private int failedOnboardings;
    private List<OnboardingRowResult> results;
}