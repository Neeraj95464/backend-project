package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.LocationAssignment;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.enums.TicketCategory;
import AssetManagement.AssetManagement.enums.TicketStatus;
import AssetManagement.AssetManagement.repository.TicketRepository;
import AssetManagement.AssetManagement.service.TicketService;
import AssetManagement.AssetManagement.service.UserAssetService;
import AssetManagement.AssetManagement.util.AuthUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-assets")
public class UserAssetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAssetController.class);

    @Autowired
    private UserAssetService userAssetService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketService ticketService;

    @GetMapping("/assets")
    public ResponseEntity<List<AssetDTO>> getUserAssets() {
        String employeeId = AuthUtils.getAuthenticatedUsername(); // ✅ Get logged-in user

        if (!StringUtils.hasText(employeeId)) {
            return ResponseEntity.badRequest().body(Collections.emptyList()); // ✅ Handle null case
        }

        List<AssetDTO> userAssets = userAssetService.getAssetsForUser(employeeId);
        return ResponseEntity.ok(userAssets);
    }

    @GetMapping("/current-user")
    public ResponseEntity<UserDTO> getCurrentUser() {
        UserDTO userDTO = userAssetService.getCurrentUser();
        return ResponseEntity.ok(userDTO);
    }

    @GetMapping("/admin/tickets")
    public ResponseEntity<PaginatedResponse<TicketDTO>> getAllTicketsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TicketStatus status) {

        PaginatedResponse<TicketDTO> response = ticketService.getAllTicketsForAdmin(page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<TicketDTO>> searchTickets(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        // Determine if the query is a valid number (for ticketId)
        Long ticketId = null;
        String title = null;

        if (query != null) {
            try {
                // Try to parse the query as a number
                ticketId = Long.parseLong(query);
            } catch (NumberFormatException e) {
                // If it can't be parsed as a number, treat it as a title
                title = query;
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Call the service method to get the paginated results
        Page<TicketDTO> pageResult = ticketService.searchTickets(ticketId, title, pageable);

        // Create the PaginatedResponse
        PaginatedResponse<TicketDTO> response = new PaginatedResponse<>(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/tickets/{ticketId}/location/{locationId}")
    public ResponseEntity<TicketDTO> updateTicketLocation(
            @PathVariable Long ticketId,
            @PathVariable Long locationId
    ) {
        TicketDTO ticketDTO = ticketService.updateLocation(ticketId, locationId);
        return ResponseEntity.ok(ticketDTO);
    }

    @PutMapping("/tickets/{ticketId}/category/{category}")
    public ResponseEntity<TicketDTO> updateTicketCategory(
            @PathVariable Long ticketId,
            @PathVariable TicketCategory category
    ) {

//        System.out.println("Request received with "+ticketId+ " "+category);

        TicketDTO updatedTicket = ticketService.updateCategory(ticketId, category);
        return ResponseEntity.ok(updatedTicket);
    }



    @GetMapping("/tickets")
    public ResponseEntity<PaginatedResponse<TicketDTO>> getUserTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false, defaultValue = "ALL") String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
//        System.out.println("Request came with employee id to fetch tickets "+employeeId);
        PaginatedResponse<TicketDTO> response = ticketService.getUserTickets(status, employeeId, page, size);
//        System.out.println(response);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/tickets/download-this-month")
    public void downloadTicketsThisMonth(HttpServletResponse response) throws IOException {
        List<TicketDTO> allTickets = ticketService.getAllTickets(); // Use your method that fetches all tickets

        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<TicketDTO> filteredTickets = allTickets.stream()
                .filter(ticket -> ticket.getCreatedAt() != null &&
                        !ticket.getCreatedAt().isBefore(startOfMonth) &&
                        !ticket.getCreatedAt().isAfter(now))
                .collect(Collectors.toList());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=tickets_this_month.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tickets");

            String[] headers = {
                    "ID", "Title", "Description", "Category", "Status", "Employee", "Created By",
                    "Assignee", "Asset Tag", "Asset Name", "Location", "Department",
                    "Created At", "Updated At", "Due Date", "Closed At"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (TicketDTO dto : filteredTickets) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getId());
                row.createCell(1).setCellValue(dto.getTitle());
                row.createCell(2).setCellValue(dto.getDescription());
                row.createCell(3).setCellValue(dto.getCategory() != null ? dto.getCategory().name() : "");
                row.createCell(4).setCellValue(dto.getStatus() != null ? dto.getStatus().name() : "");
                row.createCell(5).setCellValue(dto.getEmployee().getUsername());
                row.createCell(6).setCellValue(dto.getCreatedBy());
                row.createCell(7).setCellValue(dto.getAssignee().getUsername());
                row.createCell(8).setCellValue(dto.getAssetTag());
                row.createCell(9).setCellValue(dto.getAssetName());
                row.createCell(10).setCellValue(dto.getLocationName());
                row.createCell(11).setCellValue(dto.getTicketDepartment() != null ? dto.getTicketDepartment().name() : "");
                row.createCell(12).setCellValue(dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : "");
                row.createCell(13).setCellValue(dto.getUpdatedAt() != null ? dto.getUpdatedAt().toString() : "");
                row.createCell(14).setCellValue(dto.getDueDate() != null ? dto.getDueDate().toString() : "");
                row.createCell(15).setCellValue(dto.getClosedAt() != null ? dto.getClosedAt().toString() : "");
            }

            workbook.write(response.getOutputStream());
        }
    }


    @PutMapping("/{id}/cc-email")
    public ResponseEntity<List<String>> updateCcEmail(
            @PathVariable Long id,
            @RequestBody TicketCcEmailUpdateRequest request) {
        System.out.println("request received "+request +" "+id);
        List<String> updatedCcList = ticketService.updateCcEmail(id, request);
        return ResponseEntity.ok(updatedCcList);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<Ticket> updateTicket(
            @PathVariable("id") Long ticketId,
            @RequestBody TicketUpdateRequest request
    ) {
        Ticket updatedTicket = ticketService.updateTicket(ticketId, request);
        return ResponseEntity.ok(updatedTicket);
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<TicketDTO> getTicketById(@PathVariable Long id) {
        TicketDTO ticket = ticketService.getTicketById(id);
        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ticket);
    }


    @PutMapping("/tickets/{ticketId}/status")
    public ResponseEntity<TicketDTO> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestParam TicketStatus status) {

        try {
//            System.out.println("request came to close "+ticketId +" "+status);
            TicketDTO updatedTicket = ticketService.updateTicketStatus(ticketId, status);
            return ResponseEntity.ok(updatedTicket);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); // Handle invalid status values
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/tickets/status/{status}")
        public ResponseEntity<List<TicketDTO>> getUserTicketsByStatus(@PathVariable String status) {
            String employeeId = AuthUtils.getAuthenticatedUsername();

            if (!StringUtils.hasText(employeeId)) {
                return ResponseEntity.badRequest().body(Collections.emptyList());
            }

            try {
                TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase()); // ✅ Convert string to Enum
                List<TicketDTO> tickets = ticketService.getUserTicketsByStatus(employeeId, ticketStatus);
                return ResponseEntity.ok(tickets);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Collections.emptyList()); // ✅ Handle invalid status
            }
        }


    @PostMapping(value = "/tickets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketDTO> createTicket(
            @RequestPart("ticket") TicketDTO ticketDTO,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment) {

        TicketDTO savedTicket = ticketService.createTicket(ticketDTO, attachment);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTicket);
    }

//    @PostMapping(value = "/tickets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<List<TicketDTO>> createBulkTicket(
//            @RequestPart("ticket") List<Ticket> ticketList,
//            @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
//
//        List<TicketDTO> savedTickets = (List<TicketDTO>) ticketService.createBulkTicket(ticketList, attachment);
//        return ResponseEntity.status(HttpStatus.CREATED).body(savedTickets);
//    }



    @PutMapping("/tickets/{ticketId}/assign/{assigneeId}")
    public ResponseEntity<TicketDTO> assignTicket(@PathVariable Long ticketId, @PathVariable String assigneeId) {
        TicketDTO updatedTicket = ticketService.assignTicket(ticketId, assigneeId);
        return ResponseEntity.ok(updatedTicket);
    }

    @GetMapping("/assignees")
    public ResponseEntity<List<UserIdNameDTO>> getAllUserIdAndNames() {
        List<UserIdNameDTO> users = ticketService.getAllUserIdAndNames();
        return ResponseEntity.ok(users);
    }

//    @GetMapping("/assignees")
//    public ResponseEntity<List<LocationAssignmentDTOResponse>> getAllAssigneesLocationAndDepartmentWise() {
//        List<LocationAssignmentDTOResponse> assignments = ticketService.getAllLocationDepartmentAssignments();
//        System.out.println("Sending assignees data ");
//        return ResponseEntity.ok(assignments);
//    }


    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<TicketMessageDTO> addMessage(@PathVariable Long ticketId, @RequestBody TicketMessageDTO messageDTO) {

        TicketMessageDTO savedMessage = ticketService.addMessage(ticketId,messageDTO);
        return ResponseEntity.ok(savedMessage);
    }

    @PutMapping("/{id}/due-date")
    public ResponseEntity<String> updateDueDate(
            @PathVariable Long id,
            @RequestBody UpdateDueDateRequest request
    ) {
        ticketService.updateDueDate(id, request.getDueDate());
        return ResponseEntity.ok("Due date updated successfully");
    }

    @PutMapping("/{ticketId}/change-employee-if-same")
    public ResponseEntity<String> updateEmployeeIfAssigneeSame(
            @PathVariable Long ticketId,
            @RequestParam String newEmployeeId) {

        ticketService.updateEmployeeIfSameAsAssignee(ticketId, newEmployeeId);
        return ResponseEntity.ok("Updation success ");
    }
    @GetMapping("/tickets/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) throws IOException {
        return ticketService.downloadAttachment(id);
    }

    @PostMapping("/location-assignments")
    public ResponseEntity<LocationAssignment> assignLocation(
            @RequestBody LocationAssignmentRequest request
    ) {
        return ticketService.assignLocation(request);
    }

    @GetMapping("/all/locations-assignments")
    public List<LocationAssignmentDTO> getAllAssignments() {
        return ticketService.getAllAssignments();
    }

//    @GetMapping("/all/locations-assignments")
//    public ResponseEntity<List<LocationAssignmentDTO>> getAllAssignments() {
//        List<LocationAssignmentDTO> assignments = ticketService.getAllLocationDepartmentAssignments();
//        System.out.println("Sending assignees data from all api ");
//        return ResponseEntity.ok(assignments);
//    }

    @PostMapping("/assign")
    public ResponseEntity<String> assign(@RequestBody LocationAssignmentRequest request) {
        ticketService.assignLocation(request);
        return ResponseEntity.ok("Assignment successful");
    }

    @GetMapping("/by-site")
    public PaginatedResponse<TicketDTO> getTicketsBySiteWithDate(
            @RequestParam(required = false) Long siteId,
            @RequestParam(defaultValue = "OPEN") TicketStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {

        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }


        Page<TicketDTO>  ticketPage =  ticketService.getTicketsBySiteWithDate(
                siteId, status, startDate, endDate, page,size);

        return new PaginatedResponse<>(
                ticketPage.getContent(),
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages(),
                ticketPage.isLast()
        );
    }

    @GetMapping("/tickets/feedbacks")
    public PaginatedResponse<TicketWithFeedbackDTO> getTicketsWithFeedback(
            @RequestParam(required = false) String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TicketWithFeedbackDTO> ticketPage = ticketService.getTicketsWithFeedback(employeeId, page, size);

        return new PaginatedResponse<>(
                ticketPage.getContent(),
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages(),
                ticketPage.isLast()
        );
    }















// visualise part data apies.

    @GetMapping("/tickets/stats/status")
    public ResponseEntity<Map<TicketStatus, Long>> getTicketCountByStatus() {
        Map<TicketStatus, Long> stats = ticketService.getTicketCountByStatus();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/tickets/stats/created-per-day")
    public ResponseEntity<Map<String, Long>> getTicketsCreatedPerDay() {

        Map<String, Long> stats = ticketService.getTicketsCreatedPerDay();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/tickets/stats/category")
    public ResponseEntity<Map<String, Long>> getTicketCountByCategory() {

        Map<String, Long> stats = ticketService.getTicketCountByCategory();
        return ResponseEntity.ok(stats);
    }
    @GetMapping("/tickets/stats/assignee")
    public ResponseEntity<Map<String, Long>> getTicketCountByAssignee() {

        Map<String, Long> stats = ticketService.getTicketCountByAssignee(); // Key = assignee name or ID
        return ResponseEntity.ok(stats);
    }
    @GetMapping("/tickets/stats/resolution-time")
    public ResponseEntity<ResolutionTimeStatsDTO> getTicketResolutionTimeStats() {

        ResolutionTimeStatsDTO stats = ticketService.getResolutionTimeStats(); // avg, min, max durations
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/tickets/stats/resolution/assignee/{employeeId}")
    public ResolutionTimeStatsDTO getResolutionStatsByAssignee(@PathVariable String employeeId) {

        return ticketService.getAssigneeResolutionStats(employeeId);
    }

    @GetMapping("/tickets/stats/top-reporters")
    public ResponseEntity<Map<String, Long>> getTopTicketReporters() {
        Map<String, Long> reporters = ticketService.getTopTicketReporters();
        return ResponseEntity.ok(reporters);
    }

    @GetMapping("/tickets/stats/by-location")
    public ResponseEntity<Map<String, Long>> getTicketCountByLocation() {
        return ResponseEntity.ok(ticketService.getTicketCountByLocation());
    }

    // TicketFeedbackController.java

    @GetMapping("/feedback/all")
    public ResponseEntity<List<AssigneeFeedbackStatsDTO>> getAllFeedbacksGroupedByAssignee() {
        return ResponseEntity.ok(ticketService.getFeedbackGroupedByAssignee());
    }


    @GetMapping("/updated/filter")
    public PaginatedResponse<TicketDTO> filterTickets(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long siteIdLocationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        System.out.println("Request received ");
        return ticketService.filterTickets(title, status, category, employeeId, locationId, assigneeId, createdAfter, createdBefore,search,siteIdLocationId, page, size);
    }


    @GetMapping("/updated/download")
    public ResponseEntity<byte[]> downloadFilteredTickets(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long siteIdLocationId
    ) throws IOException {

        ByteArrayInputStream in = ticketService.exportTicketsToExcel(title, status, category, employeeId, locationId, assigneeId, createdAfter, createdBefore,search,siteIdLocationId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }

}
