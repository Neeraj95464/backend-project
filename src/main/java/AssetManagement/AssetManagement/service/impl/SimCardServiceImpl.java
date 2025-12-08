// package AssetManagement.AssetManagement.service.impl;
package AssetManagement.AssetManagement.service.impl;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.*;
import AssetManagement.AssetManagement.enums.SimStatus;
import AssetManagement.AssetManagement.mapper.CugSimMapper;
import AssetManagement.AssetManagement.repository.*;
import AssetManagement.AssetManagement.service.SimCardService;
import AssetManagement.AssetManagement.util.AuthUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SimCardServiceImpl implements SimCardService {

    private final SimCardRepository simCardRepository;
    private final SimCardHistoryRepository simCardHistoryRepository;
    private final UserRepository userRepository; // assume exists in your project
    private final LocationRepository locationRepository; // optional
    private final SiteRepository siteRepository; // optional
    private final AuthUtils authUtils;

    @Override
    public SimCardResponseDto createSimCard(SimCardRequestDto request) {
        SimCard s = new SimCard();
        mapRequestToEntity(request, s);

        // resolve assigned user if provided
        if (request.getAssignedUserId() != null) {
            User u = userRepository.findById(request.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + request.getAssignedUserId()));
            s.setAssignedUser(u);
            s.setAssignedAt(LocalDateTime.now());
            s.setStatus(SimStatus.ASSIGNED);
        }

        SimCard saved = simCardRepository.save(s);
        if (saved.getAssignedUser() != null) {
            // create history event
            createHistory(saved, "ASSIGNED", "Assigned to user id: " + saved.getAssignedUser().getId(), request.getAssignedUserId() != null ? "system" : null);
        } else {
            createHistory(saved, "CREATED", "SIM created", request.getAssignedUserId() == null ? "system" : null);
        }

        return CugSimMapper.toDTO(saved);
    }

    @Override
    public SimCardResponseDto updateSimCard(Long id, SimCardRequestDto request) {
        SimCard s = simCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SimCard not found: " + id));
        // capture before state for history:
        String before = s.toString();

        mapRequestToEntity(request, s);

        SimCard saved = simCardRepository.save(s);

        createHistory(saved, "UPDATED", "Updated fields. Before: " + before, request.getPhoneNumber() != null ? request.getPhoneNumber() : "system");
        return CugSimMapper.toDTO(saved);
    }

    @Override
    public SimCardResponseDto getSimCard(Long id) {
        SimCard s = simCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SimCard not found: " + id));
        return CugSimMapper.toDTO(s);
    }

    @Override
    public List<SimCardResponseDto> listAllSimCards() {
        return simCardRepository.findAll().stream().map(CugSimMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    public void deleteSimCard(Long id) {
        SimCard s = simCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SimCard not found: " + id));
        // If you want soft delete use a flag; currently we'll set status to DISCARDED and save
        s.setStatus(SimStatus.DISCARDED);
        simCardRepository.save(s);
        createHistory(s, "DELETED", "Marked as DISCARDED", "system");
    }

    @Override
    public SimCardResponseDto assignSimCard(Long id, SimCardAssignDto assignDto) {
        String user = AuthUtils.getAuthenticatedUserExactName();
        SimCard s = simCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SimCard not found: " + id));

        if (s.getStatus() == SimStatus.ASSIGNED) {
            throw new RuntimeException("SimCard already assigned to user id: " + (s.getAssignedUser() != null ? s.getAssignedUser().getId() : "unknown"));
        }

        User u = userRepository.findByEmployeeId(assignDto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("User not found: " + assignDto.getEmployeeId()));

        s.setAssignedUser(u);
        s.setAssignedAt(LocalDateTime.now());
        s.setStatus(SimStatus.ASSIGNED);
        if (assignDto.getNote() != null && !assignDto.getNote().isBlank()) {
            s.setNote(assignDto.getNote());
        }

        SimCard saved = simCardRepository.save(s);

        if (saved.getStatus() == SimStatus.ASSIGNED) {
            saved.setAssignmentUploaded(false); // reset
        }

        createHistory(saved, "ASSIGNED", "Assigned to user id: " + u.getEmployeeId() + " (" + u.getUsername() + ")", user);

        return CugSimMapper.toDTO(saved);
    }

    @Override
    public SimCardResponseDto unassignSimCard(Long id, String performedBy) {
        String user = AuthUtils.getAuthenticatedUserExactName();
        SimCard s = simCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SimCard not found: " + id));

        if (s.getAssignedUser() == null) {
            throw new RuntimeException("SimCard is not assigned.");
        }

        String details = "Unassigned from user id: " + s.getAssignedUser().getEmployeeId() + " (" + s.getAssignedUser().getUsername() + ")";
        s.setAssignedUser(null);
        s.setAssignedAt(null);
        s.setStatus(SimStatus.AVAILABLE);

        SimCard saved = simCardRepository.save(s);

        if (saved.getStatus() == SimStatus.AVAILABLE) {
            saved.setAssignmentUploaded(true); // reset
        }

        createHistory(saved, "UNASSIGNED", details, user);
        return CugSimMapper.toDTO(saved);
    }

    @Override
    public List<SimCardHistoryDto> getHistory(Long id) {
        List<SimCardHistory> list = simCardHistoryRepository.findBySimCardIdOrderByPerformedAtDesc(id);
        return list.stream().map(h -> {
            SimCardHistoryDto dto = new SimCardHistoryDto();
            dto.setId(h.getId());
            dto.setEventType(h.getEventType());
            dto.setDetails(h.getDetails());
            dto.setPerformedBy(h.getPerformedBy());
            dto.setPerformedAt(h.getPerformedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    /* -------------------- Helpers -------------------- */

    private void createHistory(SimCard sim, String eventType, String details, String performedBy) {
        SimCardHistory h = new SimCardHistory();
        h.setSimCard(sim);
        h.setEventType(eventType);
        h.setDetails(details);
        h.setPerformedBy(performedBy);
        h.setPerformedAt(LocalDateTime.now());
        simCardHistoryRepository.save(h);
    }

    private void mapRequestToEntity(SimCardRequestDto req, SimCard s) {
//        if (req.getSimTag() != null) s.setSimTag(req.getSimTag());
        if (req.getPhoneNumber() != null) s.setPhoneNumber(req.getPhoneNumber());
        if (req.getIccid() != null) s.setIccid(req.getIccid());
        if (req.getImsi() != null) s.setImsi(req.getImsi());
        if (req.getProvider() != null) s.setProvider(req.getProvider());
        if (req.getStatus() != null) s.setStatus(req.getStatus());
        if (req.getActivatedAt() != null) s.setActivatedAt(req.getActivatedAt());
        if (req.getPurchaseDate() != null) s.setPurchaseDate(req.getPurchaseDate());
        if (req.getPurchaseFrom() != null) s.setPurchaseFrom(req.getPurchaseFrom());
        if (req.getCost() != null) s.setCost(req.getCost());
        if (req.getNote() != null) s.setNote(req.getNote());

//        if (req.getLocationName() != null) {
//            locationRepository
//                    .findByNameAndSite(req.getLocationName(),siteRepository
//                            .findByName(req.getSiteName())).ifPresent(s::setLocation);
//        }

        if (req.getLocationName() != null && req.getSiteName() != null) {
            siteRepository.findByName(req.getSiteName())
                    .ifPresent(site ->
                            locationRepository
                                    .findByNameAndSite(req.getLocationName(), site)
                                    .ifPresent(s::setLocation)
                    );
        }

        if (req.getSiteName() != null) {
            siteRepository.findByName(req.getSiteName()).ifPresent(s::setSite);
        }
    }

//    private SimCardResponseDto mapEntityToResponse(SimCard s) {
//        SimCardResponseDto r = new SimCardResponseDto();
//        r.setId(s.getId());
////        r.setSimTag(s.getSimTag());
//        r.setAssignmentUploaded(s.getAssignmentUploaded());
//        r.setPhoneNumber(s.getPhoneNumber());
//        r.setIccid(s.getIccid());
//        r.setImsi(s.getImsi());
//        r.setProvider(s.getProvider());
//        r.setStatus(s.getStatus());
//        if (s.getAssignedUser() != null) {
//            r.setAssignedUserId(s.getAssignedUser().getId());
//            r.setAssignedUserName(s.getAssignedUser().getUsername());
//        }
//        r.setAssignedAt(s.getAssignedAt());
//        r.setActivatedAt(s.getActivatedAt());
//        r.setPurchaseDate(s.getPurchaseDate());
//        r.setPurchaseFrom(s.getPurchaseFrom());
//        r.setCost(s.getCost());
//        r.setLocationId(s.getLocation() != null ? s.getLocation().getId() : null);
//        r.setSiteId(s.getSite() != null ? s.getSite().getId() : null);
//        r.setNote(s.getNote());
//        r.setCreatedAt(s.getCreatedAt());
//        return r;
//    }


        public PaginatedResponse<SimCardResponseDto> filterSims(
                String phoneNumber,
                String provider,
                String status,
                String employeeId,
                Long departmentId,
                Long siteId,
                Long locationId,
                String search,
                LocalDateTime createdAfter,
                LocalDateTime createdBefore,
                int page,
                int size
        ) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

            Page<SimCard> data = simCardRepository.filterSims(
                    phoneNumber,
                    provider,
                    status,
                    employeeId,
                    departmentId,
                    siteId,
                    locationId,
                    search,
                    createdAfter,
                    createdBefore,
                    pageable
            );

            return new PaginatedResponse<>(
                    data.getContent().stream().map(CugSimMapper::toDTO).toList(),
                    data.getNumber(),
                    data.getSize(),
                    data.getTotalElements(),
                    data.getTotalPages(),
                    data.isLast()
            );
        }

}
