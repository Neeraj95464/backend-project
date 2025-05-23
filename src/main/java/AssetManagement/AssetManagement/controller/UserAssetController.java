package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.enums.TicketStatus;
import AssetManagement.AssetManagement.service.TicketService;
import AssetManagement.AssetManagement.service.UserAssetService;
import AssetManagement.AssetManagement.util.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-assets")
public class UserAssetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAssetController.class);

    @Autowired
    private UserAssetService userAssetService;

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
    @GetMapping("/tickets")
    public ResponseEntity<PaginatedResponse<TicketDTO>> getUserTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PaginatedResponse<TicketDTO> response = ticketService.getUserTickets( status, page, size);
        return ResponseEntity.ok(response);
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

    @PostMapping("/tickets")
    public ResponseEntity<TicketDTO> createTicket(@RequestBody TicketDTO ticketDTO) {
        TicketDTO savedTicket = ticketService.createTicket(ticketDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTicket);
    }

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

    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<TicketMessageDTO> addMessage(@PathVariable Long ticketId, @RequestBody TicketMessageDTO messageDTO) {
        String employeeId = AuthUtils.getAuthenticatedUsername();
        TicketMessageDTO savedMessage = ticketService.addMessage(ticketId, messageDTO.getMessage(), employeeId);
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




// visualise part data apies.

    @GetMapping("/tickets/stats/status")
    public ResponseEntity<Map<TicketStatus, Long>> getTicketCountByStatus() {
        Map<TicketStatus, Long> stats = ticketService.getTicketCountByStatus();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/tickets/stats/created-per-day")
    public ResponseEntity<Map<String, Long>> getTicketsCreatedPerDay() {

        Map<String, Long> stats = ticketService.getTicketsCreatedPerDay(); // Format date as yyyy-MM-dd
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
    @GetMapping("/tickets/stats/status-over-time")
    public ResponseEntity<List<TicketStatusTimeSeriesDTO>> getTicketStatusOverTime() {
        List<TicketStatusTimeSeriesDTO> data = ticketService.getTicketStatusOverTime();
        return ResponseEntity.ok(data);
    }

}
