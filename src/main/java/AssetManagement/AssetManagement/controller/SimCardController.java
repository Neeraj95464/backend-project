// package AssetManagement.AssetManagement.controller;
package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.SimAttachment;
import AssetManagement.AssetManagement.service.SimAttachmentService;
import AssetManagement.AssetManagement.service.SimCardImportService;
import AssetManagement.AssetManagement.service.SimCardService;
import AssetManagement.AssetManagement.service.impl.SimCardServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @GetMapping("/filter/export")
    public void exportFilteredCugSimsToExcel(
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAfter,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdBefore,
            HttpServletResponse response
    ) throws IOException {

        List<SimCardResponseDto> sims = simCardServiceImpl.filterSimsForExport(
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

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = "cug_sims_" + LocalDateTime.now() + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CUG Sims");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Phone Number");
            header.createCell(1).setCellValue("Provider");
            header.createCell(2).setCellValue("Status");
            header.createCell(3).setCellValue("Assignee Name");
            header.createCell(4).setCellValue("Assignee Designation");
            header.createCell(5).setCellValue("Site");
            header.createCell(6).setCellValue("Location");
            header.createCell(7).setCellValue("Created At");

            int rowIdx = 1;
            for (SimCardResponseDto sim : sims) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sim.getPhoneNumber());
                // for enums, use name (string) instead of ordinal so Excel is readable
                row.createCell(1).setCellValue(
                        sim.getProvider() != null ? sim.getProvider().name() : ""
                );
                row.createCell(2).setCellValue(
                        sim.getStatus() != null ? sim.getStatus().name() : ""
                );
                row.createCell(3).setCellValue(
                        sim.getAssignedUserName() != null ? sim.getAssignedUserName() : ""
                );
                row.createCell(4).setCellValue(
                        sim.getAssigneeDesignation() != null ? sim.getAssigneeDesignation() : ""
                );
                row.createCell(5).setCellValue(
                        sim.getSiteName() != null ? sim.getSiteName() : ""
                );
                row.createCell(6).setCellValue(
                        sim.getLocationName() != null ? sim.getLocationName() : ""
                );
                row.createCell(7).setCellValue(
                        sim.getCreatedAt() != null ? sim.getCreatedAt().toString() : ""
                );
            }

            for (int i = 0; i <= 7; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
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
