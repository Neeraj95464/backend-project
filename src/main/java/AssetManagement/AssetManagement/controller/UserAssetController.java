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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

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
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) TicketStatus status) {

        PaginatedResponse<TicketDTO> response = ticketService.getAllTicketsForAdmin(page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public List<TicketDTO> searchTickets(
            @RequestParam(required = false) Long ticketId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String employee,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String assetTag,
            @RequestParam(required = false) Long location) {

        return ticketService.searchTickets(ticketId, title, category, status, employee, assignee, assetTag, location);
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<TicketDTO>> getUserTickets(
            @RequestParam(required = false) TicketStatus status) {

        String employeeId = AuthUtils.getAuthenticatedUsername();
        if (!StringUtils.hasText(employeeId)) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        List<TicketDTO> tickets = ticketService.getUserTickets(employeeId, status);
        return ResponseEntity.ok(tickets);
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

//        @GetMapping("/admin/tickets/status/{status}")
//        public ResponseEntity<List<TicketDTO>> getAllTicketsByStatus(@PathVariable String status) {
//            try {
//                TicketStatus ticketStatus = TicketStatus.valueOf(status.toUpperCase());
//                List<TicketDTO> tickets = ticketService.getAllTicketsByStatus(ticketStatus);
//                return ResponseEntity.ok(tickets);
//            } catch (IllegalArgumentException e) {
//                return ResponseEntity.badRequest().body(Collections.emptyList());
//            }
//        }

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

    @PostMapping("/tickets/{ticketId}/messages")
    public ResponseEntity<TicketMessageDTO> addMessage(@PathVariable Long ticketId, @RequestBody TicketMessageDTO messageDTO) {
        String employeeId = AuthUtils.getAuthenticatedUsername();
        TicketMessageDTO savedMessage = ticketService.addMessage(ticketId, messageDTO.getMessage(), employeeId);
        return ResponseEntity.ok(savedMessage);
    }
}
