// package AssetManagement.AssetManagement.controller;
package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.SimAttachment;
import AssetManagement.AssetManagement.service.SimAttachmentService;
import AssetManagement.AssetManagement.service.SimCardImportService;
import AssetManagement.AssetManagement.service.SimCardService;
import AssetManagement.AssetManagement.service.impl.SimCardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sim-cards")
@RequiredArgsConstructor
public class SimCardController {

    private final SimCardService simCardService;
    private final SimCardServiceImpl simCardServiceImpl;
    private final SimAttachmentService attachmentService;
    private final SimCardImportService simImportService;

    @PostMapping
    public ResponseEntity<SimCardResponseDto> create(@RequestBody SimCardRequestDto request) {
        SimCardResponseDto resp = simCardService.createSimCard(request);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SimCardResponseDto> update(@PathVariable Long id, @RequestBody SimCardRequestDto request) {
        SimCardResponseDto resp = simCardService.updateSimCard(id, request);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SimCardResponseDto> get(@PathVariable Long id) {
        SimCardResponseDto resp = simCardService.getSimCard(id);
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<List<SimCardResponseDto>> list() {
        return ResponseEntity.ok(simCardService.listAllSimCards());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        simCardService.deleteSimCard(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<SimCardResponseDto> assign(@PathVariable Long id, @RequestBody SimCardAssignDto assignDto) {
        SimCardResponseDto resp = simCardService.assignSimCard(id, assignDto);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/unassign")
    public ResponseEntity<SimCardResponseDto> unassign(@PathVariable Long id, @RequestParam(required = false) String performedBy) {
        SimCardResponseDto resp = simCardService.unassignSimCard(id, performedBy);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<SimCardHistoryDto>> history(@PathVariable Long id) {
        return ResponseEntity.ok(simCardService.getHistory(id));
    }


        @GetMapping("/filter")
        public PaginatedResponse<SimCardResponseDto> filterCugSims(
                @RequestParam(required = false) String phoneNumber,
                @RequestParam(required = false) String provider,        // Jio / Airtel / VI
                @RequestParam(required = false) String status,          // Active / Deactive / Ported
                @RequestParam(required = false) String employeeId,
                @RequestParam(required = false) Long departmentId,
                @RequestParam(required = false) Long siteId,
                @RequestParam(required = false) Long locationId,
                @RequestParam(required = false) String search,           // universal search
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                LocalDateTime createdAfter,
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                LocalDateTime createdBefore,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size
        ) {
            return simCardServiceImpl.filterSims(
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
                    page,
                    size
            );
        }

    @PostMapping("attachments/{simId}")
    public ResponseEntity<?> uploadSimAttachment(
            @PathVariable Long simId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "note", required = false) String note
    ) {
        try {
            SimAttachmentDto saved = attachmentService.uploadAttachment(simId, file, note);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/attachments/{simId}")
    public ResponseEntity<List<SimAttachmentDto>> getAttachments(@PathVariable Long simId) {
        return ResponseEntity.ok(attachmentService.getAttachments(simId));
    }

    // 2️⃣ Download attachment
    @GetMapping("/attachments/download/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        try {
            Resource file = attachmentService.download(attachmentId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getFilename() + "\"")
                    .body(file);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @PostMapping("/excel")
    public ResponseEntity<SimImportResult> importSims(@RequestParam("file") MultipartFile file) {
        SimImportResult result = simImportService.importSimsFromExcel(file);
        return ResponseEntity.ok(result);
    }

}
