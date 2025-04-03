package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.PaginatedResponse;
import AssetManagement.AssetManagement.dto.TicketCategory;
import AssetManagement.AssetManagement.dto.TicketDTO;
import AssetManagement.AssetManagement.dto.TicketMessageDTO;
import AssetManagement.AssetManagement.entity.*;
import AssetManagement.AssetManagement.enums.TicketStatus;
import AssetManagement.AssetManagement.exception.UserNotFoundException;
import AssetManagement.AssetManagement.mapper.TicketMapper;
import AssetManagement.AssetManagement.repository.*;
import AssetManagement.AssetManagement.util.AuthUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;
    private final LocationAssignmentRepository locationAssignmentRepository;
    private final TicketMapper ticketMapper;
    private final LocationRepository locationRepository;
    private final EmailTicketService emailTicketService;

    public TicketService(TicketRepository ticketRepository, TicketMessageRepository ticketMessageRepository, UserRepository userRepository, AssetRepository assetRepository, LocationAssignmentRepository locationAssignmentRepository, TicketMapper ticketMapper, LocationRepository locationRepository, EmailTicketService emailTicketService, EmailTicketService emailTicketService1) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
        this.locationAssignmentRepository = locationAssignmentRepository;
        this.ticketMapper = ticketMapper;
        this.locationRepository = locationRepository;
        this.emailTicketService = emailTicketService1;
    }

    @Transactional
    public TicketDTO createTicket(TicketDTO ticketDTO) {
        Ticket ticket = new Ticket();
        ticket.setTitle(ticketDTO.getTitle());
        ticket.setDescription(ticketDTO.getDescription());
        ticket.setCategory(ticketDTO.getCategory());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy(AuthUtils.getAuthenticatedUserExactName());

        // ✅ Fetch and set location
        Location location = locationRepository.findById(ticketDTO.getLocation())
                .orElseThrow(() -> new EntityNotFoundException("Location not found"));
        ticket.setLocation(location);

        // ✅ Fetch and set Employee
        User employeeUser = userRepository.findByEmployeeId(ticketDTO.getEmployee())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        ticket.setEmployee(employeeUser);

        // ✅ Set Asset if provided
        assetRepository.findByAssetTag(ticketDTO.getAssetTag()).ifPresent(ticket::setAsset);

        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        // ✅ Auto-assign IT executive based on location
        User assignee = findITExecutiveByLocation(location.getId()); // Auto-assign
        ticket.setAssignee(assignee); // Even if null, it's fine

        Ticket savedTicket = ticketRepository.save(ticket);
        // ✅ Send email notification to the ticket creator
        emailTicketService.sendTicketCreationEmail(employeeUser.getEmail(), ticket);
        return convertTicketToDTO(savedTicket);
    }



    public List<TicketDTO> searchTickets(Long ticketId, String title, String category, TicketStatus status, String employee, String assignee, String assetTag, Long location) {

        if (ticketId != null) { // If ticket ID is provided, return only that ticket
            Optional<Ticket> ticketOptional = ticketRepository.findById(ticketId);
            return ticketOptional.map(ticket -> List.of(convertTicketToDTO(ticket))).orElse(List.of());
        }

        List<Ticket> tickets = ticketRepository.findAll();

        return tickets.stream()
                .filter(ticket -> (title == null || ticket.getTitle().toLowerCase().contains(title.toLowerCase())))
                .filter(ticket -> (category == null || ticket.getCategory().toString().equalsIgnoreCase(category)))
                .filter(ticket -> (status == null || ticket.getStatus() == status))
                .filter(ticket -> (employee == null || ticket.getEmployee().getUsername().equalsIgnoreCase(employee)))
                .filter(ticket -> (assignee == null || (ticket.getAssignee() != null && ticket.getAssignee().getUsername().equalsIgnoreCase(assignee))))
                .filter(ticket -> (assetTag == null || (ticket.getAsset() != null && ticket.getAsset().getAssetTag().equalsIgnoreCase(assetTag))))
                .filter(ticket -> (location == null || ticket.getLocation().equals(location)))
                .map(this::convertTicketToDTO)
                .collect(Collectors.toList());
    }

    public PaginatedResponse<TicketDTO> getAllTicketsForAdmin(int page, int size, TicketStatus status) {
        // Ensure only admin users can access this data
        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString();
        if (!role.contains("ADMIN")) {
            throw new RuntimeException("Access Denied: Only admins can view all tickets.");
        }

        Page<Ticket> ticketPage;

        if (status != null) {
            // Fetch tickets based on status
            ticketPage = ticketRepository.findByStatus(status, PageRequest.of(page, size));
        } else {
            // Fetch all tickets
            ticketPage = ticketRepository.findAll(PageRequest.of(page, size));
        }

        List<TicketDTO> ticketDTOs = ticketPage.getContent().stream()
                .map(ticketMapper::toDTO)
                .collect(Collectors.toList());

        // Return a paginated response
        return new PaginatedResponse<>(
                ticketDTOs,
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages(),
                ticketPage.isLast()
        );
    }

    private User findITExecutiveByLocation(Long locationId) {
        // Fetch all IT executives responsible for this location
        List<User> executives = locationAssignmentRepository.findExecutivesByLocation(locationId);

        if (executives.isEmpty()) {
            return null; // ❌ No IT executive available for this location
        }
        return executives.stream()
                .min(Comparator.comparingInt(this::getAssignedTicketsCount))
                .orElse(null);
    }

    private int getAssignedTicketsCount(User executive) {
        return ticketRepository.countByAssignee(executive);
    }

    @Transactional
    public TicketDTO assignTicket(Long ticketId, String empId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        User assignee = userRepository.findByEmployeeId(empId)
                .orElseThrow(() -> new RuntimeException("Assignee user not found"));

        ticket.setAssignee(assignee);
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket updatedTicket = ticketRepository.save(ticket);
        return convertTicketToDTO(updatedTicket);
    }

    @Transactional
    public TicketMessageDTO addMessage(Long ticketId, String message, String employeeId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        User senderUser = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        TicketMessage ticketMessage = new TicketMessage();
        ticketMessage.setTicket(ticket);
        ticketMessage.setMessage(message);
        ticketMessage.setSender(senderUser);
        ticketMessage.setSentAt(LocalDateTime.now());

        TicketMessage savedMessage = ticketMessageRepository.save(ticketMessage);
        emailTicketService.sendReplyToTicket(ticketId,ticketMessage.getMessage());
        return convertMessageToDTO(savedMessage);
    }

//    public TicketDTO updateTicketStatus(Long ticketId, TicketStatus status) {
//        Ticket ticket = ticketRepository.findById(ticketId)
//                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
//
//        ticket.setStatus(status);
//        ticketRepository.save(ticket);
//
//        return ticketMapper.toDTO(ticket);
//    }

    public TicketDTO updateTicketStatus(Long ticketId, TicketStatus newStatus) {
        String updatedByEmployeeId = AuthUtils.getAuthenticatedUsername();
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        User updater = userRepository.findByEmployeeId(updatedByEmployeeId)
                .orElseThrow(() -> new UserNotFoundException("Updater not found"));

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        // Save ticket first before creating message
        ticketRepository.save(ticket);

        // Create a new TicketMessage entry for status update
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(updater); // The user updating the status
        message.setMessage("Status changed from " + oldStatus + " to " + newStatus);
        message.setSentAt(LocalDateTime.now());
        message.setStatusUpdate(newStatus);
        message.setStatusUpdatedBy(updater);

        ticketMessageRepository.save(message);

        return convertTicketToDTO(ticket);
    }



//    public List<TicketDTO> getUserTickets(String employeeId) {
//        User user = userRepository.findByEmployeeId(employeeId)
//                .orElseThrow(() -> new UserNotFoundException("User not found"));
//
//        List<Ticket> tickets = ticketRepository.findByEmployee(user);
//
//        if (tickets == null || tickets.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        return tickets.stream().map(this::convertTicketToDTO).collect(Collectors.toList());
//    }

    public List<TicketDTO> getUserTickets(String employeeId, TicketStatus status) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<Ticket> tickets;

        if (status != null) {
            tickets = ticketRepository.findByEmployeeAndStatus(user, status);
            tickets.addAll(ticketRepository.findByAssigneeAndStatus(user, status)); // Also fetch assigned tickets
        } else {
            tickets = ticketRepository.findByEmployee(user);
            tickets.addAll(ticketRepository.findByAssignee(user)); // Also fetch assigned tickets
        }

        return tickets.stream().distinct().map(this::convertTicketToDTO).collect(Collectors.toList());
    }



        public List<TicketDTO> getUserTicketsByStatus(String employeeId, TicketStatus status) {
            User user = userRepository.findByEmployeeId(employeeId)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            List<Ticket> tickets = ticketRepository.findByEmployeeAndStatus(user, status);

            if (tickets == null || tickets.isEmpty()) {
                return Collections.emptyList();
            }

            return tickets.stream().map(this::convertTicketToDTO).collect(Collectors.toList());
        }

//        public List<TicketDTO> getAllTicketsByStatus(TicketStatus status) {
//            List<Ticket> tickets = ticketRepository.findByStatus(status);
//
//            if (tickets == null || tickets.isEmpty()) {
//                return Collections.emptyList();
//            }
//
//            return tickets.stream().map(this::convertTicketToDTO).collect(Collectors.toList());
//        }

    public List<TicketDTO> getAssignedTickets(String assignee) {
        User assigneeUser = userRepository.findByUsername(assignee)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<Ticket> tickets = ticketRepository.findByAssignee(assigneeUser);
        return tickets.stream().map(this::convertTicketToDTO).collect(Collectors.toList());
    }

    private TicketDTO convertTicketToDTO(Ticket ticket) {
        List<TicketMessageDTO> messages = ticketMessageRepository.findByTicket(ticket)
                .stream()
                .map(this::convertMessageToDTO)
                .collect(Collectors.toList());

        return new TicketDTO(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),

                ticket.getStatus(),
                ticket.getEmployee() != null ? ticket.getEmployee().getEmployeeId() : null, // ✅ Employee ID instead of username
                ticket.getCreatedBy(),
                ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : null, // ✅ Assignee Username
                ticket.getAsset() != null ? ticket.getAsset().getAssetTag() : null, // ✅ Asset ID
                ticket.getAsset() != null ? ticket.getAsset().getName() : "Other", // ✅ Asset Name or "Other"
                ticket.getLocation() != null ? ticket.getLocation().getId():null,
                ticket.getCcEmails(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                messages
        );
    }

    private TicketMessageDTO convertMessageToDTO(TicketMessage ticketMessage) {
        return new TicketMessageDTO(
                ticketMessage.getId(),
                ticketMessage.getMessage(),
                ticketMessage.getSender().getUsername(), // ✅ Convert User to username
                ticketMessage.getSentAt()
        );
    }
}
