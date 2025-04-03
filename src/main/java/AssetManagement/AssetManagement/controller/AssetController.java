package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.AssetHistory;
import AssetManagement.AssetManagement.enums.AssetStatus;
import AssetManagement.AssetManagement.exception.AssetNotFoundException;
import AssetManagement.AssetManagement.repository.AssetHistoryRepository;
import AssetManagement.AssetManagement.repository.AssetRepository;
import AssetManagement.AssetManagement.service.AssetService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets")
//@CrossOrigin(origins = "http://localhost:5173/")
public class AssetController {

    private final AssetService assetService;
    private final AssetHistoryRepository assetHistoryRepository;
    private final AssetRepository assetRepository;

    public AssetController(AssetService assetService, AssetHistoryRepository assetHistoryRepository, AssetRepository assetRepository) {
        this.assetService = assetService;
        this.assetHistoryRepository = assetHistoryRepository;
        this.assetRepository = assetRepository;
    }

    @GetMapping
    public ResponseEntity<List<AssetDTO>> getAllAssets() {
        try {
            List<AssetDTO> assets = assetService.getAllAssets();
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getAssetCounts() {
        Map<String, Long> assetCounts = assetService.getAssetCounts();
        return ResponseEntity.ok(assetCounts);
    }
    @GetMapping("/status/{status}")
    public List<AssetDTO> getAssetsByStatus(@PathVariable AssetStatus status) {
        return assetService.getAssetsByStatus(status);
    }

    @GetMapping("/{assetTagOrSerial}")
    public ResponseEntity<AssetDTO> getAssetByAssetTagOrSerial(@PathVariable String assetTagOrSerial) {
        try {
            Optional<AssetDTO> asset = assetService.getAssetByAssetTagOrSerial(assetTagOrSerial);
            return asset.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (Exception e) {
            e.printStackTrace(); // Log for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/search")
    public List<Asset> searchAssets(@RequestParam("query") String query) {
        return assetService.searchAssets(query);
    }

    @PostMapping
    public ResponseEntity<AssetDTO> createAsset(@RequestBody Asset asset) {
        try {
            AssetDTO savedAsset = assetService.saveAsset(asset);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAsset);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{parentId}/add-child")
    public ResponseEntity<Asset> addChildAsset(@PathVariable Long parentId, @RequestBody Asset childAsset) {
        Asset parent = assetRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent Asset not found"));

        childAsset.setParentAsset(parent);
        Asset savedChild = assetRepository.save(childAsset);

        return ResponseEntity.ok(savedChild);
    }

    @GetMapping("/{id}/children")
    public List<Asset> getChildAssets(@PathVariable Long id) {
        Asset parent = assetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        return parent.getChildAssets();
    }

    @PutMapping("/{assetTag}")
    public ResponseEntity<AssetDTO> updateAsset(@PathVariable String assetTag, @RequestBody Asset asset) {
        try {
//            System.out.println("Request reveived with "+id +" asset is "+asset);
            AssetDTO updatedAsset = assetService.editAsset(assetTag, asset);
//            System.out.println("\n updated asset is "+updatedAsset);
            return ResponseEntity.ok(updatedAsset);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @PutMapping("/{assetTag}/reset-status")
    public ResponseEntity<String> resetAssetStatus(
            @PathVariable String assetTag,
            @RequestParam(value = "statusNote") String statusNote,
            @RequestParam(value= "modifiedBy", required = false) String modifiedBy) {
        try {
            assetService.resetAssetStatus(assetTag, statusNote);
            return ResponseEntity.ok("Asset status reset successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to reset asset status: " + e.getMessage());
        }
    }

    @PutMapping("/{assetTag}/status")
    public ResponseEntity<AssetDTO> updateAssetStatus(
            @PathVariable String assetTag,
            @RequestParam AssetStatus newStatus,
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reservationStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reservationEndDate,
            @RequestParam(required = false) String statusNote) {
        try {
            AssetDTO updatedAsset = assetService.updateAssetStatus(
                    assetTag, newStatus, userId, reservationStartDate, reservationEndDate, statusNote);
            return ResponseEntity.ok(updatedAsset);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{assetTag}")
    public ResponseEntity<Void> deleteAsset(@PathVariable String assetTag) {
        try {
            assetService.deleteAsset(assetTag);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{assetTag}/history")
    public ResponseEntity<List<AssetHistoryDTO>> getAssetHistory(@PathVariable String assetTag) {
        // Fetch history from repository
        List<AssetHistory> historyList = assetHistoryRepository.findByAsset_AssetTagOrderByModifiedAtDesc(assetTag);

        // Convert entity list to DTO list
        List<AssetHistoryDTO> historyDTOs = historyList.stream()
                .map(history -> new AssetHistoryDTO(
                        history.getChangedAttribute(),  // Field that was changed (e.g., status, location, assignedUser)
                        history.getOldValue(),      // Previous value
                        history.getNewValue(),      // Updated value
                        history.getModifiedBy(),    // User who made the change
                        history.getModifiedAt()      // When the change happened
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(historyDTOs);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AssetDTO>> getAssetsByUser(@PathVariable Long userId) {
        try {
            List<AssetDTO> assets = assetService.getAssetsByUser(userId);
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/location/{locationId}")
    public ResponseEntity<List<AssetDTO>> getAssetsByLocation(@PathVariable Long locationId) {
        try {
            List<AssetDTO> assets = assetService.getAssetsByLocation(locationId);
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{assetTag}/status/in-repair")
    public ResponseEntity<?> updateAssetToInRepair(
            @PathVariable String assetTag,
            @RequestParam(required = false) Long userId,
            @RequestParam String statusNote,
            @RequestParam(required = false, defaultValue = "false") boolean markAsRepaired) {
        try {
            AssetDTO updatedAsset = assetService.updateAssetToInRepair(assetTag, userId, statusNote, markAsRepaired);
            return ResponseEntity.ok(updatedAsset);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid input: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Asset not found or cannot be updated."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }
//    @GetMapping("/asset-tag")
//    public ResponseEntity<AssetDTO> getAssetByAssetTag(@RequestParam String assetTag) {
//        AssetDTO assetDTO = assetService.getAssetByAssetTag(assetTag);
//        if (assetDTO != null) {
//            return ResponseEntity.ok(assetDTO);
//        } else {
//            return ResponseEntity.notFound().build();
//        }
//    }

    @PostMapping("/{assetTag}/checkin")
    public ResponseEntity<AssetDTO> checkInAsset(
            @PathVariable String assetTag,
            @RequestBody CheckInDTO checkInDTO) {
        AssetDTO updatedAsset = assetService.checkInAsset(assetTag, checkInDTO);
        return ResponseEntity.ok(updatedAsset);
    }
    @PutMapping("/dispose/{assetTag}")
    public ResponseEntity<AssetDTO> disposeAsset(
            @PathVariable String assetTag,
            @RequestBody Map<String, String> requestBody) {

        String statusNote = requestBody.get("statusNote");

        AssetDTO updatedAsset = assetService.dispose(assetTag, statusNote);
        return ResponseEntity.ok(updatedAsset);
    }
    @PutMapping("/reserve/{assetTag}")
    public ResponseEntity<Map<String, String>> reserveAsset(
            @PathVariable String assetTag,
            @RequestBody AssetReservationRequest request) {

        assetService.reserveAsset(assetTag, request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Asset reserved successfully!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/lost/{assetTag}")
    public ResponseEntity<?> markAssetAsLost(
            @PathVariable String assetTag,
            @RequestBody Map<String, String> requestBody) {
        try {
            String statusNote = requestBody.get("statusNote");

            AssetDTO updatedAsset = assetService.markAsLost(assetTag, statusNote);
            return ResponseEntity.ok(Map.of("message", "Asset marked as lost successfully!", "asset", updatedAsset));
        } catch (AssetNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Asset not found: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error marking asset as lost: " + e.getMessage()));
        }
    }

    @PostMapping("/{assetTag}/checkout")
    public ResponseEntity<String> checkOutAsset(
            @PathVariable String assetTag,
            @RequestBody CheckOutDTO checkOutDTO) {

        try {
            // Assign the asset using CheckOutDTO
            AssetDTO assignedAsset = assetService.assignAssetToUser(assetTag, checkOutDTO);

            return ResponseEntity.ok(
                    "Asset checked out successfully to " + checkOutDTO.getAssignedTo().getUsername()
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Asset or User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while checking out the asset.");
        }
    }
}
