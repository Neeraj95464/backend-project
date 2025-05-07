package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.*;
import AssetManagement.AssetManagement.enums.TicketDepartment;
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
import java.util.*;
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
        ticket.setCreatedBy(AuthUtils.getAuthenticatedUserExactName());
        ticket.setTicketDepartment(ticketDTO.getTicketDepartment());

        // ✅ Fetch and set location
        Location location = locationRepository.findById(ticketDTO.getLocation())
                .orElseThrow(() -> new EntityNotFoundException("Location not found"));
        ticket.setLocation(location);

        // ✅ Fetch and set employee
        User employeeUser = userRepository.findByEmployeeId(ticketDTO.getEmployee())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        ticket.setEmployee(employeeUser);

        // ✅ Set asset if provided
        assetRepository.findByAssetTag(ticketDTO.getAssetTag())
                .ifPresent(ticket::setAsset);

        // ✅ Auto-assign IT executive based on location
        User assignee = findExecutiveByLocationAndDepartment(location.getId(),ticketDTO.getTicketDepartment());
        ticket.setAssignee(assignee);

        // ✅ Set status based on whether an assignee is available
        if (assignee == null) {
            ticket.setStatus(TicketStatus.UNASSIGNED);
        } else {
            ticket.setStatus(TicketStatus.OPEN);
        }
        List<String> ccEmails = new ArrayList<>();

// Add existing emails first, if any
        if (ticket.getCcEmails() != null) {
            ccEmails.addAll(ticket.getCcEmails());
        }

        if (assignee != null) {
            ccEmails.add(assignee.getEmail());
        }

        ticket.setCcEmails(ccEmails);

        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket savedTicket = ticketRepository.save(ticket);

        // ✅ Send acknowledgment email to the ticket creator
        emailTicketService.sendTicketAcknowledgmentEmail(
                employeeUser.getEmail(),
                ticket,
                ticket.getCcEmails(),
                null,
                null
        );

        return convertTicketToDTO(savedTicket);
    }
    public TicketDTO getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElse(null);
        if (ticket == null) {
            return null;
        }
        return ticketMapper.toDTO(ticket); // Assumes you're using a mapper to convert entity to DTO
    }

    @Transactional
    public Ticket updateTicket(Long ticketId, TicketUpdateRequest request) {
        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);
        if (optionalTicket.isEmpty()) {
            throw new RuntimeException("Ticket not found with ID: " + ticketId);
        }

        Ticket ticket = optionalTicket.get();

        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
        }

        if (request.getCcEmails() != null) {
            ticket.setCcEmails(request.getCcEmails());
        }

        if (request.getLocationId() != null) {
            Location location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new RuntimeException("Location not found"));
            ticket.setLocation(location);
        }

        return ticketRepository.save(ticket);
    }


    public List<TicketDTO> searchTickets(Long ticketId, String title, String category,
                                         TicketStatus status, String employee, String assignee,
                                         String assetTag, Long locationId) {

        if (ticketId != null) {
            return ticketRepository.findById(ticketId)
                    .map(ticket -> List.of(convertTicketToDTO(ticket)))
                    .orElse(List.of());
        }

        List<Ticket> tickets = ticketRepository.findAll();

        return tickets.stream()
                .filter(ticket -> title == null ||
                        (ticket.getTitle() != null && ticket.getTitle().toLowerCase().contains(title.toLowerCase())))
                .filter(ticket -> category == null ||
                        (ticket.getCategory() != null && ticket.getCategory().name().equalsIgnoreCase(category)))
                .filter(ticket -> status == null || ticket.getStatus() == status)
                .filter(ticket -> employee == null ||
                        (ticket.getEmployee() != null && ticket.getEmployee().getUsername().equalsIgnoreCase(employee)))
                .filter(ticket -> assignee == null ||
                        (ticket.getAssignee() != null && ticket.getAssignee().getUsername().equalsIgnoreCase(assignee)))
                .filter(ticket -> assetTag == null ||
                        (ticket.getAsset() != null && ticket.getAsset().getAssetTag().equalsIgnoreCase(assetTag)))
                .filter(ticket -> locationId == null ||
                        (ticket.getLocation() != null && ticket.getLocation().getId().equals(locationId)))
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

//    private User findITExecutiveByLocation(Long locationId) {
//        // Fetch all IT executives responsible for this location
//        List<User> executives = locationAssignmentRepository.findExecutivesByLocation(locationId);
//
//        if (executives.isEmpty()) {
//            return null; // ❌ No IT executive available for this location
//        }
//        return executives.stream()
//                .min(Comparator.comparingInt(this::getAssignedTicketsCount))
//                .orElse(null);
//    }

    private User findExecutiveByLocationAndDepartment(Long locationId, TicketDepartment department) {
        // Fetch executives responsible for the given department and location
        List<User> executives = locationAssignmentRepository.findExecutivesByLocationAndDepartment(locationId, department);

        if (executives.isEmpty()) {
            return null; // ❌ No executive found for this department at the location
        }

        // Choose the executive with the least assigned tickets
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

        if (assignee == null) {
            ticket.setStatus(TicketStatus.UNASSIGNED);
        } else{
            ticket.setStatus(TicketStatus.OPEN);
        }
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
        emailTicketService.sendAcknowledgmentReplyToTicket(ticketId,message,ticket.getMessageId());

        return convertMessageToDTO(savedMessage);
    }


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
                ticket.getEmployee() != null ? ticket.getEmployee().getUsername() : null, // ✅ Employee ID instead of username
                ticket.getCreatedBy(),
                ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : null, // ✅ Assignee Username
                ticket.getAsset() != null ? ticket.getAsset().getAssetTag() : null, // ✅ Asset ID
                ticket.getAsset() != null ? ticket.getAsset().getName() : "Other", // ✅ Asset Name or "Other"
                ticket.getLocation() != null ? ticket.getLocation().getName():null,
                ticket.getLocation() != null ? ticket.getLocation().getId():null,
                ticket.getTicketDepartment(),
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
