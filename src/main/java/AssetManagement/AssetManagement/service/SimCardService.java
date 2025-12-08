// package AssetManagement.AssetManagement.service;
package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.SimCard;

import java.util.List;

public interface SimCardService {

    SimCardResponseDto createSimCard(SimCardRequestDto request);

    SimCardResponseDto updateSimCard(Long id, SimCardRequestDto request);

    SimCardResponseDto getSimCard(Long id);

    List<SimCardResponseDto> listAllSimCards();

    void deleteSimCard(Long id); // soft or hard depending on your preference

    SimCardResponseDto assignSimCard(Long id, SimCardAssignDto assignDto);

    SimCardResponseDto unassignSimCard(Long id, String performedBy);

    List<SimCardHistoryDto> getHistory(Long id);
}
