package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.AssetReservationRequest;
import AssetManagement.AssetManagement.dto.CheckInDTO;
import AssetManagement.AssetManagement.dto.CheckOutDTO;
import AssetManagement.AssetManagement.entity.*;
import AssetManagement.AssetManagement.enums.AssetStatus;
import AssetManagement.AssetManagement.dto.AssetDTO;
import AssetManagement.AssetManagement.exception.AssetDisposalException;
import AssetManagement.AssetManagement.exception.AssetNotFoundException;
import AssetManagement.AssetManagement.exception.UserNotFoundException;
import AssetManagement.AssetManagement.repository.*;
import AssetManagement.AssetManagement.util.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final SiteRepository siteRepository;
    private final LocationRepository locationRepository;
    private final AssetHistoryService assetHistoryService;
    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

    public AssetService(AssetRepository assetRepository, UserRepository userRepository, AssetHistoryRepository assetHistoryRepository, SiteRepository siteRepository, LocationRepository locationRepository, AssetHistoryService assetHistoryService) {
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.assetHistoryRepository = assetHistoryRepository;
        this.siteRepository = siteRepository;
        this.locationRepository = locationRepository;
        this.assetHistoryService = assetHistoryService;
    }
    public AssetDTO dispose(String assetTag, String statusNote) {
        String modifiedBy=AuthUtils.getAuthenticatedUserExactName();
        Optional<Asset> optionalAsset = assetRepository.findByAssetTag(assetTag);

        if (optionalAsset.isEmpty()) {
            throw new AssetNotFoundException("Asset not found with ID: " + assetTag);
        }

        Asset asset = optionalAsset.get();

        // Prevent disposal if the asset is assigned to a user
        if (asset.getAssignedUser() != null) {
            throw new AssetDisposalException("Cannot dispose asset as it is assigned to a user.");
        }

        // Prevent disposal if the asset is already disposed
        if (asset.getStatus() == AssetStatus.DISPOSED) {
            throw new AssetDisposalException("Asset is already disposed.");
        }

        // Capture old value before updating
        String oldStatus = asset.getStatus().name();

        // Update asset details
        asset.setStatus(AssetStatus.DISPOSED);
//        asset.setLastUpdate(LocalDateTime.now());
        asset.setStatusNote(statusNote);

        Asset updatedAsset = assetRepository.save(asset);

        // Save asset history for status change
        assetHistoryService.saveAssetHistory(asset, "status", oldStatus, AssetStatus.DISPOSED.name(), modifiedBy);
        assetHistoryService.saveAssetHistory(asset, "statusNote", "", statusNote, modifiedBy);

        return convertAssetToDto(updatedAsset);
    }

    public AssetDTO markAsLost(String assetTag, String statusNote) {
        String modifiedBy=AuthUtils.getAuthenticatedUserExactName();
        Optional<Asset> optionalAsset = assetRepository.findByAssetTag(assetTag);

        if (optionalAsset.isEmpty()) {
            throw new AssetNotFoundException("Asset not found with ID: " + assetTag);
        }

        Asset asset = optionalAsset.get();

        // Prevent marking as lost if assigned to a user
        if (asset.getAssignedUser() != null) {
            throw new AssetDisposalException("Cannot mark asset as lost as it is assigned to a user.");
        }

        // Prevent redundant status update
        if (asset.getStatus() == AssetStatus.LOST) {
            throw new AssetDisposalException("Asset is already marked as lost.");
        }

        // ✅ Track changes before updating
        assetHistoryService.saveAssetHistory(asset, "status", asset.getStatus().name(), AssetStatus.LOST.name(), modifiedBy);
        assetHistoryService.saveAssetHistory(asset, "statusNote", asset.getStatusNote(), statusNote, modifiedBy);

        // ✅ Update the asset
        asset.setStatus(AssetStatus.LOST);
//        asset.setLastUpdate(LocalDateTime.now());
        asset.setStatusNote(statusNote);

        Asset updatedAsset = assetRepository.save(asset);
        return convertAssetToDto(updatedAsset);
    }

    @Transactional
    public AssetDTO reserveAsset(String assetTag, AssetReservationRequest request) {
        String modifiedBy=AuthUtils.getAuthenticatedUserExactName();
        Asset asset = assetRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new AssetNotFoundException("Asset not found with ID: " + assetTag));

        // Validate asset status before reserving
        if (asset.getStatus() == AssetStatus.RESERVED) {
            throw new IllegalArgumentException("Asset is already reserved.");
        }
        if (asset.getAssignedUser() != null) {
            throw new IllegalArgumentException("Cannot reserve asset as it is currently assigned to a user.");
        }
        if (asset.getStatus() == AssetStatus.LOST || asset.getStatus() == AssetStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("Asset cannot be reserved due to its current status: " + asset.getStatus());
        }

        // Validate reservation request (either for user or site, but not both)
        boolean isUserReservation = request.getReservedForUserId() != null;
        boolean isSiteReservation = request.getReserveForSiteId() != null;

        if (isUserReservation && isSiteReservation) {
            throw new IllegalArgumentException("Asset cannot be reserved for both a user and a site at the same time.");
        }
        if (!isUserReservation && !isSiteReservation) {
            throw new IllegalArgumentException("Asset must be reserved for either a user or a site.");
        }

        // Fetch user or site based on reservation type
        User reservedUser = isUserReservation
                ? userRepository.findById(request.getReservedForUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + request.getReservedForUserId()))
                : null;

        Site reservedSite = isSiteReservation
                ? siteRepository.findById(request.getReserveForSiteId())
                .orElseThrow(() -> new IllegalArgumentException("Site not found with ID: " + request.getReserveForSiteId()))
                : null;

        Location reservedLocation = null;
        if (reservedSite != null) {
            reservedLocation = locationRepository.findById(request.getReserveForLocationId())
                    .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + request.getReserveForLocationId()));
        }

        // Capture old values for history
        String oldStatus = asset.getStatus().name();
        String oldUser = asset.getAssignedUser() != null ? asset.getAssignedUser().getUsername() : "None";
        String oldSite = asset.getSite() != null ? asset.getSite().getName() : "None";
        String oldLocation = asset.getLocation() != null ? asset.getLocation().getName() : "None";
        String oldStartDate = asset.getReservationStartDate() != null ? asset.getReservationStartDate().toString() : "None";
        String oldEndDate = asset.getReservationEndDate() != null ? asset.getReservationEndDate().toString() : "None";

        // Set reservation details
        asset.setReservationStartDate(request.getReservationStartDate());
        asset.setReservationEndDate(request.getReservationEndDate());
        asset.setStatus(AssetStatus.RESERVED);
        asset.setStatusNote(request.getStatusNote());

        if (isUserReservation) {
            asset.setAssignedUser(reservedUser);
        } else {
            asset.setAssignedUser(null);
            asset.setSite(reservedSite);
            asset.setLocation(reservedLocation);
        }

        // Save the updated asset
        Asset updatedAsset = assetRepository.save(asset);

        // Save changes to history
        assetHistoryService.saveAssetHistory(asset, "status", oldStatus, AssetStatus.RESERVED.name(), modifiedBy);
        assetHistoryService.saveAssetHistory(asset, "reservationStartDate", oldStartDate, request.getReservationStartDate().toString(), modifiedBy);
        assetHistoryService.saveAssetHistory(asset, "reservationEndDate", oldEndDate, request.getReservationEndDate().toString(), modifiedBy);

        if (isUserReservation) {
            assetHistoryService.saveAssetHistory(asset, "assignedUser", oldUser, reservedUser.getUsername(), modifiedBy);
        } else {
            assetHistoryService.saveAssetHistory(asset, "site", oldSite, reservedSite.getName(), modifiedBy);
            assetHistoryService.saveAssetHistory(asset, "location", oldLocation, reservedLocation.getName(), modifiedBy);
        }

        // Convert to DTO and return
        return convertAssetToDto(updatedAsset);
    }

    public Optional<AssetDTO> getAssetBySerialNumber(String serialNumber) {
        Optional<Asset> searchedAsset = assetRepository.findBySerialNumber(serialNumber);

        // Convert only if present, otherwise return empty Optional
        return searchedAsset.map(this::convertAssetToDto);
    }

    // Retrieve all assets
    public List<AssetDTO> getAllAssets() {
        return assetRepository.findAll().stream()
                .map(this::convertAssetToDto)
                .collect(Collectors.toList());
    }

    // Save a new asset
        @Transactional
        public AssetDTO saveAsset(Asset asset) {
            // Validate uniqueness before saving
            validateUniqueFields(asset);

            asset.setCreatedBy(AuthUtils.getAuthenticatedUserExactName());
            asset.setCreatedAt(LocalDateTime.now());

            Asset savedAsset = assetRepository.save(asset);

            return convertAssetToDto(savedAsset);
        }

        private void validateUniqueFields(Asset asset) {
            Optional<Asset> existingBySerial = assetRepository.findBySerialNumber(asset.getSerialNumber());
            if (existingBySerial.isPresent() && !existingBySerial.get().getId().equals(asset.getId())) {
                throw new IllegalArgumentException("Serial Number must be unique. Duplicate found!");
            }

            Optional<Asset> existingByTag = assetRepository.findByAssetTag(asset.getAssetTag());
            if (existingByTag.isPresent() && !existingByTag.get().getId().equals(asset.getId())) {
                throw new IllegalArgumentException("Asset Tag must be unique. Duplicate found!");
            }
        }

    // Retrieve an asset by ID
    public Optional<AssetDTO> getAssetById(String assetTag) {
        return assetRepository.findByAssetTag(assetTag).map(this::convertAssetToDto);
    }

    public void deleteAsset(String assetTag) {
        String modifiedBy=AuthUtils.getAuthenticatedUserExactName();
        Asset asset = assetRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new AssetNotFoundException("Asset not found with ID: " + assetTag));

        // 🚨 Prevent deletion if the asset is assigned or reserved
        if (asset.getAssignedUser() != null) {
            throw new IllegalArgumentException("Cannot delete asset as it is assigned to a user.");
        }
        if (asset.getStatus() == AssetStatus.RESERVED) {
            throw new IllegalArgumentException("Cannot delete asset as it is currently reserved.");
        }

        // 📜 Save deletion history
        assetHistoryService.saveAssetHistory(asset, "status", asset.getStatus().name(), "DELETED", modifiedBy);

        // ⚠️ Option 1: Soft Delete (Recommended)
        asset.setStatus(AssetStatus.DELETED);
        assetRepository.save(asset);

        // ❌ Option 2: Hard Delete (Use with Caution)
        // assetRepository.delete(asset);
    }

    // Update asset status
    @Transactional
    public AssetDTO updateAssetStatus(String assetTag, AssetStatus newStatus, Long userId, LocalDate reservationStartDate,
                                      LocalDate reservationEndDate, String statusNote) {
        String modifiedBy=AuthUtils.getAuthenticatedUserExactName();
        Asset asset = findAssetById(assetTag);

        validateAssetStatus(newStatus, userId, reservationStartDate, reservationEndDate, statusNote,asset);

        // Save current state to AssetHistory
//        assetHistoryRepository.save(new AssetHistory(asset));

        switch (newStatus) {
            case IN_REPAIR -> asset.setStatusNote(statusNote);
            case RESERVED -> {
                asset.setReservationStartDate(reservationStartDate);
                asset.setReservationEndDate(reservationEndDate);
                asset.setStatusNote(statusNote);
            }
            case CHECKED_IN -> resetAssetStatus(assetTag, statusNote);
            default -> throw new IllegalArgumentException("Invalid asset status.");
        }

        asset.setStatus(newStatus);
        Asset updatedAsset = assetRepository.save(asset);
        return convertAssetToDto(updatedAsset);
    }

    @Transactional
    public AssetDTO editAsset(String assetTag, Asset updatedAsset) {
        String modifiedBy=AuthUtils.getAuthenticatedUserExactName();
        if (assetTag == null || updatedAsset == null || modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid input parameters: Asset ID, updated asset, and modifiedBy must not be null or empty.");
        }

        Asset existingAsset = findAssetById(assetTag);

        updateAssetDetails(existingAsset, updatedAsset, modifiedBy);

        // 💾 Save updated asset
        Asset savedAsset = assetRepository.save(existingAsset);

        logger.info("Asset [{}] updated by [{}]", existingAsset.getId(), modifiedBy);
        return convertAssetToDto(savedAsset);
    }

    private void updateAssetDetails(Asset existingAsset, Asset updatedAsset, String modifiedBy) {
        if (existingAsset == null || updatedAsset == null) {
            throw new IllegalArgumentException("Existing and updated assets must not be null.");
        }

        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user cannot be null or empty.");
        }

        // Track and update changes (only non-null values)
        trackAndUpdate(existingAsset, updatedAsset, "Name", existingAsset.getName(), updatedAsset.getName(), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Description", existingAsset.getDescription(), updatedAsset.getDescription(), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Serial Number", existingAsset.getSerialNumber(), updatedAsset.getSerialNumber(), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Purchase Date", formatDate(existingAsset.getPurchaseDate()), formatDate(updatedAsset.getPurchaseDate()), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Purchase From", existingAsset.getPurchaseFrom(), updatedAsset.getPurchaseFrom(), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Status", formatEnum(existingAsset.getStatus()), formatEnum(updatedAsset.getStatus()), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Brand", existingAsset.getBrand(), updatedAsset.getBrand(), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Model", existingAsset.getModel(), updatedAsset.getModel(), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Asset Type", formatEnum(existingAsset.getAssetType()), formatEnum(updatedAsset.getAssetType()), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Department", formatEnum(existingAsset.getDepartment()), formatEnum(updatedAsset.getDepartment()), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Cost", formatCost(existingAsset.getCost()), formatCost(updatedAsset.getCost()), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Location", formatLocation(existingAsset.getLocation()), formatLocation(updatedAsset.getLocation()), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Site", formatId(existingAsset.getSite()), formatId(updatedAsset.getSite()), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Assigned User", formatUser(existingAsset.getAssignedUser()), formatUser(updatedAsset.getAssignedUser()), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Reservation Start Date", formatDate(existingAsset.getReservationStartDate()), formatDate(updatedAsset.getReservationStartDate()), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Reservation End Date", formatDate(existingAsset.getReservationEndDate()), formatDate(updatedAsset.getReservationEndDate()), modifiedBy);
        trackAndUpdate(existingAsset, updatedAsset, "Status Note", existingAsset.getStatusNote(), updatedAsset.getStatusNote(), modifiedBy);

        // Apply updates (only non-null values)
        applyUpdates(existingAsset, updatedAsset);
    }

    private void trackAndUpdate(Asset existingAsset, Asset updatedAsset, String fieldName, String oldValue, String newValue, String modifiedBy) {
        if (newValue != null && !Objects.equals(oldValue, newValue)) {
            assetHistoryService.saveAssetHistory(existingAsset, fieldName, oldValue, newValue, modifiedBy);
        }
    }

    private void applyUpdates(Asset existingAsset, Asset updatedAsset) {
        if (updatedAsset.getName() != null) existingAsset.setName(updatedAsset.getName());
        if (updatedAsset.getDescription() != null) existingAsset.setDescription(updatedAsset.getDescription());
        if (updatedAsset.getSerialNumber() != null) existingAsset.setSerialNumber(updatedAsset.getSerialNumber());
        if (updatedAsset.getPurchaseDate() != null) existingAsset.setPurchaseDate(updatedAsset.getPurchaseDate());
        if (updatedAsset.getPurchaseFrom() != null) existingAsset.setPurchaseFrom(updatedAsset.getPurchaseFrom());
        if (updatedAsset.getStatus() != null) existingAsset.setStatus(updatedAsset.getStatus());
        if (updatedAsset.getBrand() != null) existingAsset.setBrand(updatedAsset.getBrand());
        if (updatedAsset.getModel() != null) existingAsset.setModel(updatedAsset.getModel());
        if (updatedAsset.getAssetType() != null) existingAsset.setAssetType(updatedAsset.getAssetType());
        if (updatedAsset.getDepartment() != null) existingAsset.setDepartment(updatedAsset.getDepartment());
        if (updatedAsset.getCost() != null) existingAsset.setCost(updatedAsset.getCost());
        if (updatedAsset.getLocation() != null) existingAsset.setLocation(updatedAsset.getLocation());
        if (updatedAsset.getSite() != null) existingAsset.setSite(updatedAsset.getSite());
        if (updatedAsset.getAssignedUser() != null) existingAsset.setAssignedUser(updatedAsset.getAssignedUser());
        if (updatedAsset.getReservationStartDate() != null) existingAsset.setReservationStartDate(updatedAsset.getReservationStartDate());
        if (updatedAsset.getReservationEndDate() != null) existingAsset.setReservationEndDate(updatedAsset.getReservationEndDate());
        if (updatedAsset.getStatusNote() != null) existingAsset.setStatusNote(updatedAsset.getStatusNote());

//        existingAsset.setLastUpdate(LocalDateTime.now());
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    private String formatEnum(Enum<?> enumValue) {
        return enumValue != null ? enumValue.name() : null;
    }

    private String formatCost(BigDecimal cost) {
        return cost != null ? cost.toString() : null;
    }

    private String formatId(Site entity) {
        return entity != null ? entity.getId().toString() : null;
    }

    private String formatUser(User user) {
        return user != null ? user.getUsername() : null;
    }

    private String formatLocation(Location location) {
        return location != null ? location.getName() : null;
    }

    // Retrieve assets assigned to a user
    public List<AssetDTO> getAssetsByUser(Long userId) {
        User user = findUserById(userId);
        return assetRepository.findByAssignedUser(user).stream()
                .map(this::convertAssetToDto)
                .collect(Collectors.toList());
    }
    // Assign an asset to a user
    @Transactional
    public AssetDTO assignAssetToUser(String assetTag, CheckOutDTO checkOutDTO) {
        // 🔍 Fetch asset from DB
        Asset asset = findAssetById(assetTag);
        String modifiedBy = AuthUtils.getAuthenticatedUserExactName();

        // 🚨 Validate if asset is already assigned
        if (asset.getAssignedUser() != null) {
            throw new IllegalStateException(
                    String.format("Asset ID %d is already assigned to %s", assetTag, asset.getAssignedUser().getUsername())
            );
        }

        // 🔍 Fetch user from DTO
        User assignedUser = checkOutDTO.getAssignedTo();
        if (assignedUser == null) {
            throw new IllegalArgumentException("Assigned user cannot be null.");
        }

        // 🚀 Capture changes before updating
        trackAssignmentChanges(asset, assignedUser, checkOutDTO, modifiedBy);

        // 🛠 Update asset details
        asset.setAssignedUser(assignedUser);
        asset.setStatus(AssetStatus.CHECKED_OUT);
        asset.setStatusNote(checkOutDTO.getCheckOutNote());
        asset.setLocation(checkOutDTO.getLocation());
        asset.setDepartment(checkOutDTO.getDepartment());

        // 💾 Save changes
        Asset updatedAsset = assetRepository.save(asset);

        // 🔄 Convert to DTO & return
        return convertAssetToDto(updatedAsset);
    }

    private void trackAssignmentChanges(Asset asset, User userDetails, CheckOutDTO checkOutDTO, String modifiedBy) {

        // Fetch user details safely
        User newUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userDetails.getId()));

        // Fetch location details safely
        Location newLocation = checkOutDTO.getLocation() != null
                ? locationRepository.findById(checkOutDTO.getLocation().getId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + checkOutDTO.getLocation().getId()))
                : null;

        // Track assigned user change
        assetHistoryService.saveAssetHistory(asset, "assignedUser",
                asset.getAssignedUser() != null ? asset.getAssignedUser().getUsername() : "Unassigned",
                newUser.getUsername(), modifiedBy);

        // Track status change
        assetHistoryService.saveAssetHistory(asset, "status",
                asset.getStatus().name(), AssetStatus.CHECKED_OUT.name(), modifiedBy);

        // Track location change (handle null values properly)
        assetHistoryService.saveAssetHistory(asset, "location",
                asset.getLocation() != null ? asset.getLocation().getName() : "Unspecified",
                newLocation != null ? newLocation.getName() : "Unspecified", modifiedBy);

        // Track department change (handle null values properly)
        assetHistoryService.saveAssetHistory(asset, "department",
                asset.getDepartment() != null ? asset.getDepartment().name() : "Unspecified",
                checkOutDTO != null ? String.valueOf(checkOutDTO.getDepartment()) : "Unspecified", modifiedBy);

        // Track status note change if present
        if (checkOutDTO.getCheckOutNote() != null) {
            assetHistoryService.saveAssetHistory(asset, "statusNote",
                    asset.getStatusNote() != null ? asset.getStatusNote() : "No previous note",
                    checkOutDTO.getCheckOutNote(), modifiedBy);
        }
    }

    @Transactional
    public AssetDTO checkInAsset(String assetTag, CheckInDTO checkInDTO) {
        String modifiedBy = AuthUtils.getAuthenticatedUserExactName();
        // 🔍 Fetch asset from DB
        Asset asset = findAssetById(assetTag);

        // 🚨 Validate if asset is already checked in
        if (asset.getAssignedUser() == null) {
            throw new IllegalStateException(String.format("Asset ID %d is not currently assigned to any user.", assetTag));
        }

        // 🔄 Capture changes before updating
        trackCheckInChanges(asset, checkInDTO, modifiedBy);

        // 🛠 Clear assignment details & update asset
        asset.setAssignedUser(null);
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setStatusNote(checkInDTO.getCheckInNote());
        asset.setLocation(checkInDTO.getLocation());
        asset.setDepartment(checkInDTO.getDepartment());
        asset.setSite(checkInDTO.getSite());

        // 💾 Save changes
        Asset updatedAsset = assetRepository.save(asset);

        // 🔄 Convert to DTO & return
        return convertAssetToDto(updatedAsset);
    }

    private void trackCheckInChanges(Asset asset, CheckInDTO checkInDTO, String modifiedBy) {
        // Fetch location details safely
        Location newLocation = checkInDTO.getLocation() != null
                ? locationRepository.findById(checkInDTO.getLocation().getId())
                .orElseThrow(() -> new IllegalArgumentException("Location not found with ID: " + checkInDTO.getLocation().getId()))
                : null;

        // Fetch site details safely
        Site newSite = checkInDTO.getSite() != null
                ? siteRepository.findById(checkInDTO.getSite().getId())
                .orElseThrow(() -> new IllegalArgumentException("Site not found with ID: " + checkInDTO.getSite().getId()))
                : null;

        // Track assigned user removal
        assetHistoryService.saveAssetHistory(asset, "assignedUser",
                asset.getAssignedUser() != null ? asset.getAssignedUser().getUsername() : "Unassigned",
                "Unassigned", modifiedBy);

        // Track status change
        assetHistoryService.saveAssetHistory(asset, "status",
                asset.getStatus().name(), AssetStatus.AVAILABLE.name(), modifiedBy);

        // Track location change (handle null values properly)
        assetHistoryService.saveAssetHistory(asset, "location",
                asset.getLocation() != null ? asset.getLocation().getName() : "Unspecified",
                newLocation != null ? newLocation.getName() : "Unspecified", modifiedBy);

        // Track site change (handle null values properly)
        assetHistoryService.saveAssetHistory(asset, "site",
                asset.getSite() != null ? asset.getSite().getName() : "Unspecified",
                newSite != null ? newSite.getName() : "Unspecified", modifiedBy);

        // Track status note change if present
        if (checkInDTO.getCheckInNote() != null) {
            assetHistoryService.saveAssetHistory(asset, "statusNote",
                    asset.getStatusNote() != null ? asset.getStatusNote() : "No previous note",
                    checkInDTO.getCheckInNote(), modifiedBy);
        }
    }


    public Map<String, Long> getAssetCounts() {
        return Map.of(
                "total", assetRepository.count(),
                "available", assetRepository.countByStatus(AssetStatus.AVAILABLE),
                "assigned", assetRepository.countByStatus(AssetStatus.CHECKED_OUT),
                "inRepair", assetRepository.countByStatus(AssetStatus.IN_REPAIR),
                "disposed", assetRepository.countByStatus(AssetStatus.DISPOSED),
                "reserved", assetRepository.countByStatus(AssetStatus.RESERVED),
                "lost", assetRepository.countByStatus(AssetStatus.LOST)
        );
    }

    public Optional<AssetDTO> getAssetByAssetTagOrSerial(String assetTagOrSerial) {
        return assetRepository.findByAssetTag(assetTagOrSerial)
                .map(this::convertAssetToDto)
                .or(() -> assetRepository.findBySerialNumber(assetTagOrSerial).map(this::convertAssetToDto)
                        );
    }

    public List<AssetDTO> getAssetsByStatus(AssetStatus status) {
        List<Asset> assets = assetRepository.findByStatus(status);

        return assets.stream()
                .map(this::convertAssetToDto)  // Using your existing conversion method
                .collect(Collectors.toList());
    }

    // Retrieve assets by location
    public List<AssetDTO> getAssetsByLocation(Long locationId) {
        return assetRepository.findByLocationId(locationId).stream()
                .map(this::convertAssetToDto)
                .collect(Collectors.toList());
    }
    public List<Asset> searchAssets(String query) {
        return assetRepository.findByNameContainingIgnoreCaseOrSerialNumberContainingIgnoreCase(query, query);
    }

    // Helper methods
    private Asset findAssetById(String assetTag) {
        return assetRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new AssetNotFoundException("Asset not found with ID: " + assetTag));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    private void validateAssetStatus(AssetStatus newStatus, Long userId, LocalDate reservationStartDate,
                                     LocalDate reservationEndDate, String statusNote, Asset asset) {
        AssetStatus existingAssetStatus = asset.getStatus();

        switch (newStatus) {
            case CHECKED_OUT -> {
                if (userId == null)
                    throw new IllegalArgumentException("User ID is required when checking out an asset.");
                if (isNullOrEmpty(statusNote))
                    throw new IllegalArgumentException("A check-out note is required.");
            }
            case IN_REPAIR -> {
                if (existingAssetStatus == AssetStatus.IN_REPAIR)
                    throw new IllegalArgumentException("This asset is already in repair.");
                if (isNullOrEmpty(statusNote))
                    throw new IllegalArgumentException("A repair note is required.");
            }
            case RESERVED -> {
                if (reservationStartDate == null || reservationEndDate == null)
                    throw new IllegalArgumentException("Both reservation start and end dates are required.");
                if (reservationStartDate.isAfter(reservationEndDate))
                    throw new IllegalArgumentException("Reservation start date cannot be after the end date.");
                if (isNullOrEmpty(statusNote))
                    throw new IllegalArgumentException("A reservation note is required.");
            }
            case CHECKED_IN -> {
                if (isNullOrEmpty(statusNote))
                    throw new IllegalArgumentException("A check-in note is required.");
            }
            default -> throw new IllegalArgumentException("Invalid asset status provided.");
        }
    }


    public void resetAssetStatus(String assetTag, String statusNote) {
        String modifiedBy = AuthUtils.getAuthenticatedUserExactName();
        Asset asset = assetRepository.findByAssetTag(assetTag).orElseThrow(()->new AssetNotFoundException("Asset not found "));
        if (asset == null) {
            throw new IllegalArgumentException("Asset cannot be null");
        }
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by user cannot be null or empty");
        }

        // Track changes in asset status and related fields
        assetHistoryService.saveAssetHistory(asset, "Status", asset.getStatus().name(), AssetStatus.AVAILABLE.name(), modifiedBy);
        assetHistoryService.saveAssetHistory(asset, "Assigned User",
                asset.getAssignedUser() != null ? asset.getAssignedUser().getUsername() : "NULL",
                "NULL", modifiedBy);
        assetHistoryService.saveAssetHistory(asset, "Reservation Start Date",
                asset.getReservationStartDate() != null ? asset.getReservationStartDate().toString() : "NULL",
                "NULL", modifiedBy);
        assetHistoryService.saveAssetHistory(asset, "Reservation End Date",
                asset.getReservationEndDate() != null ? asset.getReservationEndDate().toString() : "NULL",
                "NULL", modifiedBy);
        assetHistoryService.saveAssetHistory(asset, "Status Note",
                asset.getStatusNote() != null ? asset.getStatusNote() : "NULL",
                statusNote != null ? statusNote.trim() : "NULL", modifiedBy);

        // Reset asset fields
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setAssignedUser(null);
        asset.setReservationStartDate(null);
        asset.setReservationEndDate(null);
        asset.setStatusNote(statusNote != null ? statusNote.trim() : "");
        assetRepository.save(asset);

//        logger.info("Asset [{}] status reset to AVAILABLE by [{}]. Status Note: {}",
//                asset.getId(), modifiedBy, statusNote);
    }


    private static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public AssetDTO updateAssetToInRepair(String assetTag, Long userId, String statusNote, boolean markAsRepaired) {
        String modifiedBy = AuthUtils.getAuthenticatedUserExactName();
        // Fetch asset or throw exception if not found
        Asset asset = assetRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new AssetNotFoundException("Asset with ID " + assetTag + " not found"));

        assetHistoryService.saveAssetHistory(asset, "Status", String.valueOf(asset.getStatus()), "IN_REPAIR",modifiedBy);
        // Handle in-repair status update
        handleInRepairStatus(asset, userId, statusNote, markAsRepaired);

        // Update status note and save asset
        assetHistoryService.saveAssetHistory(asset, "statusNote", asset.getStatusNote(), statusNote,modifiedBy);

        asset.setStatusNote(statusNote);
        assetRepository.save(asset);

//        log.info("Asset [{}] marked as {} by User [{}]", assetTag, markAsRepaired ? "Repaired" : "In Repair", userId);

        return convertAssetToDto(asset);
    }


    private void handleInRepairStatus(Asset asset, Long userId, String statusNote, boolean markAsRepaired) {
        AssetStatus existingStatus = asset.getStatus();

        if (statusNote == null || statusNote.trim().isEmpty()) {
            throw new IllegalArgumentException("Repair note is required.");
        }

        if (existingStatus == AssetStatus.IN_REPAIR) {
            if (markAsRepaired) {
                if (asset.getAssignedUser() != null) {
                    asset.setStatus(AssetStatus.CHECKED_OUT);
                }else {
                    asset.setStatus(AssetStatus.AVAILABLE);
                }
            }
            return;
        }

        if (existingStatus != AssetStatus.CHECKED_OUT && existingStatus != AssetStatus.AVAILABLE) {
            throw new IllegalArgumentException("Asset can only be moved to 'IN_REPAIR' from 'CHECKED_OUT' or 'AVAILABLE'.");
        }

        asset.setStatus(AssetStatus.IN_REPAIR);
    }

    public AssetDTO getAssetByAssetTag(String assetTag) {
        Asset asset = assetRepository.findByAssetTag(assetTag)
                .orElseThrow(() -> new AssetNotFoundException("Asset not found for tag: " + assetTag));

        return convertAssetToDto(asset);
    }

    public List<Map<String, Object>> getAssetCountByType() {
        return assetRepository.countAssetsByType();
    }

    public AssetDTO convertAssetToDto(Asset asset) {
        AssetDTO dto = new AssetDTO();
        dto.setAssetTag(asset.getAssetTag());
        dto.setName(asset.getName());

        dto.setDescription(asset.getDescription());
        dto.setSerialNumber(asset.getSerialNumber());
        dto.setPurchaseDate(asset.getPurchaseDate());

        // ✅ Handle null safety for Enums
        dto.setStatus(asset.getStatus() != null ? asset.getStatus().name() : "UNKNOWN");

        // ✅ Safely handle potential null values to prevent NullPointerExceptions
        dto.setLocationName(asset.getLocation() != null ? asset.getLocation().getName() : null);

        dto.setSiteName(asset.getSite() != null ? asset.getSite().getName() : null);

        dto.setAssignedUserName(asset.getAssignedUser() != null ? asset.getAssignedUser().getUsername() : null);

        dto.setReservationStartDate(asset.getReservationStartDate());
        dto.setReservationEndDate(asset.getReservationEndDate());
        dto.setStatusNote(asset.getStatusNote());
        dto.setBrand(asset.getBrand());
        dto.setModel(asset.getModel());

        // ✅ Ensure null safety for enums
        dto.setDepartment(asset.getDepartment() != null ? asset.getDepartment().toString() : null);
        dto.setAssetType(asset.getAssetType() != null ? asset.getAssetType().toString() : null);
        dto.setCreatedAt(asset.getCreatedAt());
        dto.setCreatedBy(asset.getCreatedBy());

        return dto;
    }


//    public Map<String, BigDecimal> getCostByAssetType() {
//        return assetRepository.findAll().stream()
//                .collect(Collectors.groupingBy(
//                        asset -> asset.getAssetType().name(),
//                        Collectors.reducing(BigDecimal.ZERO, Asset::getCost, BigDecimal::add)
//                ));
//    }
public Map<String, BigDecimal> getCostByAssetType() {
    return assetRepository.findAll().stream()
            .filter(asset -> asset.getCost() != null && asset.getAssetType() != null)
            .collect(Collectors.groupingBy(
                    asset -> asset.getAssetType().name(),
                    Collectors.reducing(BigDecimal.ZERO, Asset::getCost, BigDecimal::add)
            ));
}


}
