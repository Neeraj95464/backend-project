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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SimCardServiceImpl implements SimCardService {

    private final SimCardRepository simCardRepository;
    private final SimCardHistoryRepository simCardHistoryRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final SiteRepository siteRepository;

    @Override
    public SimCardResponseDto createSimCard(SimCardRequestDto request) {
        String currentUser = AuthUtils.getAuthenticatedUserExactName();

        SimCard s = new SimCard();
        mapRequestToEntity(request, s);

        if (request.getAssignedUserId() != null) {
            User u = userRepository.findByEmployeeId(request.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + request.getAssignedUserId()));
            s.setAssignedUser(u);
            s.setAssignedAt(LocalDateTime.now());
            s.setStatus(SimStatus.ASSIGNED);
        }

        SimCard saved = simCardRepository.save(s);

        if (saved.getAssignedUser() != null) {
            createHistory(
                    saved,
                    "ASSIGNED",
                    "Assigned to user id: " + saved.getAssignedUser().getEmployeeId(),
                    currentUser
            );
        } else {
            createHistory(
                    saved,
                    "CREATED",
                    "SIM created",
                    currentUser
            );
        }

        return CugSimMapper.toDTO(saved);
    }

    @Override
    public SimCardResponseDto updateSimCard(Long id, SimCardRequestDto request) {
        if (request.getCost() != null) {
            // Remove decimals if whole number or format it
            BigDecimal cleanedCost = request.getCost().setScale(2, RoundingMode.HALF_UP);
            request.setCost(cleanedCost);
        }

        String currentUser = AuthUtils.getAuthenticatedUserExactName();

        SimCard simCard = simCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SimCard not found: " + id));

        Map<String, String> changes = trackChanges(simCard, request);

        if (changes.isEmpty()) {
            throw new RuntimeException("No changes detected");
        }

        mapRequestToEntity(request, simCard);

//        System.out.println("sim card updating data are "+simCard);
        SimCard saved = simCardRepository.save(simCard);

        // ✅ ONLY show changed fields - NO full entity dump
//        String changeDetails = changes.entrySet().stream()
//                .map(entry -> entry.getKey() + ": " + entry.getValue())
//                .collect(Collectors.joining(", "));

        String changeDetails = changes.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));


        createHistory(saved, "UPDATED", "Updated: " + changeDetails, currentUser);

        return CugSimMapper.toDTO(saved);
    }


    private Map<String, String> trackChanges(SimCard simCard, SimCardRequestDto request) {
        Map<String, String> changes = new HashMap<>();

        // Track ALL updatable fields
        if (request.getPhoneNumber() != null && !Objects.equals(request.getPhoneNumber(), simCard.getPhoneNumber())) {
            changes.put("phoneNumber", simCard.getPhoneNumber() + " → " + request.getPhoneNumber());
        }
        if (request.getIccid() != null && !Objects.equals(request.getIccid(), simCard.getIccid())) {
            changes.put("iccid", simCard.getIccid() + " → " + request.getIccid());
        }
        if (request.getImsi() != null && !Objects.equals(request.getImsi(), simCard.getImsi())) {
            changes.put("imsi", simCard.getImsi() + " → " + request.getImsi());
        }
        if (request.getProvider() != null && !Objects.equals(request.getProvider(), simCard.getProvider())) {
            changes.put("provider", simCard.getProvider() + " → " + request.getProvider());
        }
        if (request.getStatus() != null && !Objects.equals(request.getStatus(), simCard.getStatus())) {
            changes.put("status", simCard.getStatus() + " → " + request.getStatus());
        }
        if (request.getNote() != null && !Objects.equals(request.getNote(), simCard.getNote())) {
            changes.put("note", simCard.getNote() + " → " + request.getNote());
        }
        if (request.getActivatedAt() != null && !Objects.equals(request.getActivatedAt(), simCard.getActivatedAt())) {
            changes.put("activatedAt", simCard.getActivatedAt() + " → " + request.getActivatedAt());
        }
        if (request.getPurchaseDate() != null && !Objects.equals(request.getPurchaseDate(), simCard.getPurchaseDate())) {
            changes.put("purchaseDate", simCard.getPurchaseDate() + " → " + request.getPurchaseDate());
        }
        if (request.getPurchaseFrom() != null && !Objects.equals(request.getPurchaseFrom(), simCard.getPurchaseFrom())) {
            changes.put("purchaseFrom", simCard.getPurchaseFrom() + " → " + request.getPurchaseFrom());
        }
        if (request.getCost() != null && !Objects.equals(request.getCost(), simCard.getCost())) {
            changes.put("cost", simCard.getCost() + " → " + request.getCost());
        }
        if (request.getAssignedUserId() != null) {
            User oldUser = simCard.getAssignedUser();
            User newUser = userRepository.findByEmployeeId(request.getAssignedUserId()).orElse(null);
            if (!Objects.equals(oldUser != null ? oldUser.getEmployeeId() : null, request.getAssignedUserId())) {
                changes.put("assignedUser", (oldUser != null ? oldUser.getEmployeeId() : "null") + " → " + request.getAssignedUserId());
            }
        }

        // ✅ NEW: Track Site & Location changes
        if (request.getSiteName() != null && !Objects.equals(request.getSiteName(), getCurrentSiteName(simCard))) {
            changes.put("site", getCurrentSiteName(simCard) + " → " + request.getSiteName());
        }
        if (request.getLocationName() != null && !Objects.equals(request.getLocationName(), getCurrentLocationName(simCard))) {
            changes.put("location", getCurrentLocationName(simCard) + " → " + request.getLocationName());
        }

        return changes;
    }

    // Helper methods to get current site/location names from entity
    private String getCurrentSiteName(SimCard simCard) {
        return simCard.getSite() != null ? simCard.getSite().getName() : null;
    }

    private String getCurrentLocationName(SimCard simCard) {
        return simCard.getLocation() != null ? simCard.getLocation().getName() : null;
    }



    private String createAuditString(SimCard simCard) {
        return String.format("SimCard[id=%d, phone=%s, status=%s, note='%s']",
                simCard.getId(), simCard.getPhoneNumber(), simCard.getStatus(),
                simCard.getNote() != null ? simCard.getNote() : "null");
    }


    @Override
    public SimCardResponseDto getSimCard(Long id) {
        SimCard s = simCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SimCard not found: " + id));
        return CugSimMapper.toDTO(s);
    }

    @Override
    public List<SimCardResponseDto> listAllSimCards() {
        return simCardRepository.findAll()
                .stream()
                .map(CugSimMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteSimCard(Long id) {
        String currentUser = AuthUtils.getAuthenticatedUserExactName();

        SimCard s = simCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SimCard not found: " + id));

        s.setStatus(SimStatus.DISCARDED);
        simCardRepository.save(s);

        createHistory(
                s,
                "DELETED",
                "Marked as DISCARDED",
                currentUser
        );
    }

    @Override
    public SimCardResponseDto assignSimCard(Long id, SimCardAssignDto assignDto) {
        String currentUser = AuthUtils.getAuthenticatedUserExactName();

        SimCard s = simCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SimCard not found: " + id));

        if (s.getStatus() == SimStatus.ASSIGNED) {
            throw new RuntimeException(
                    "SimCard already assigned to user id: " +
                            (s.getAssignedUser() != null ? s.getAssignedUser().getId() : "unknown")
            );
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
            saved.setAssignmentUploaded(false);
        }

        createHistory(
                saved,
                "ASSIGNED",
                "Assigned to user id: " + u.getEmployeeId() + " (" + u.getUsername() + ")",
                currentUser
        );

        return CugSimMapper.toDTO(saved);
    }

    @Override
    public SimCardResponseDto unassignSimCard(Long id, String performedByIgnored) {
        String currentUser = AuthUtils.getAuthenticatedUserExactName();

        SimCard s = simCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SimCard not found: " + id));

        if (s.getAssignedUser() == null) {
            throw new RuntimeException("SimCard is not assigned.");
        }

        String details =
                "Unassigned from user id: " + s.getAssignedUser().getEmployeeId()
                        + " (" + s.getAssignedUser().getUsername() + ")";

        s.setAssignedUser(null);
        s.setAssignedAt(null);
        s.setStatus(SimStatus.AVAILABLE);

        SimCard saved = simCardRepository.save(s);

        if (saved.getStatus() == SimStatus.AVAILABLE) {
            saved.setAssignmentUploaded(true);
        }

        createHistory(
                saved,
                "UNASSIGNED",
                details,
                currentUser
        );
        return CugSimMapper.toDTO(saved);
    }

    @Override
    public List<SimCardHistoryDto> getHistory(Long id) {
        List<SimCardHistory> list =
                simCardHistoryRepository.findBySimCardIdOrderByPerformedAtDesc(id);

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


    private void mapRequestToEntity(SimCardRequestDto request, SimCard simCard) {
        if (request.getPhoneNumber() != null) {
            simCard.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getIccid() != null) {
            simCard.setIccid(request.getIccid());
        }
        if (request.getImsi() != null) {
            simCard.setImsi(request.getImsi());
        }
        if (request.getProvider() != null) {
            simCard.setProvider(request.getProvider());
        }
        if (request.getStatus() != null) {
            simCard.setStatus(request.getStatus());
        }
        if (request.getAssignedUserId() != null) {
            simCard.setAssignedUser(userRepository.findByEmployeeId(request.getAssignedUserId())
                    .orElseThrow(() -> new RuntimeException("User not found")));
        }
        if (request.getActivatedAt() != null) {
            simCard.setActivatedAt(request.getActivatedAt());
        }
        if (request.getPurchaseDate() != null) {
            simCard.setPurchaseDate(request.getPurchaseDate());
        }
        if (request.getPurchaseFrom() != null) {
            simCard.setPurchaseFrom(request.getPurchaseFrom());
        }
        if (request.getCost() != null) {
            simCard.setCost(request.getCost());
        }

        if (request.getSiteName() != null && !request.getSiteName().isBlank()) {
            Site site = siteRepository.findByName(request.getSiteName().trim())
                    .orElseThrow(() -> new RuntimeException("Site not found: '" + request.getSiteName() + "'"));

            if (request.getLocationName() != null && !request.getLocationName().isBlank()) {
                Location location = locationRepository.findByNameAndSite(request.getLocationName().trim(), site)
                        .orElseThrow(() -> new RuntimeException("Location '" + request.getLocationName() + "' not found in site '" + request.getSiteName() + "'"));
                simCard.setLocation(location);
            }
            simCard.setSite(site);
        }
        if (request.getNote() != null) {
            simCard.setNote(request.getNote());
        }
        // Update other fields as needed
    }



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

    public List<SimCardResponseDto> filterSimsForExport(
            String phoneNumber,
            String provider,
            String status,
            String employeeId,
            Long departmentId,
            Long siteId,
            Long locationId,
            String search,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        List<SimCard> data = simCardRepository.filterSimsForExport(
                phoneNumber,
                provider,
                status,
                employeeId,
                departmentId,
                siteId,
                locationId,
                search,
                createdAfter,
                createdBefore
        );

        return data.stream()
                .map(CugSimMapper::toDTO)
                .toList();
    }


}
