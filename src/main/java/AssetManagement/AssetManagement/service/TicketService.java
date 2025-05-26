package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.*;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import AssetManagement.AssetManagement.enums.TicketStatus;
import AssetManagement.AssetManagement.exception.UserNotFoundException;
import AssetManagement.AssetManagement.mapper.TicketMapper;
import AssetManagement.AssetManagement.repository.*;
import AssetManagement.AssetManagement.util.AuthUtils;
import AssetManagement.AssetManagement.util.TicketSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final EmailService emailTicketService;

    public TicketService(TicketRepository ticketRepository, TicketMessageRepository ticketMessageRepository, UserRepository userRepository, AssetRepository assetRepository, LocationAssignmentRepository locationAssignmentRepository, TicketMapper ticketMapper, LocationRepository locationRepository, EmailService emailTicketService, EmailService emailTicketService1) {
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
    public TicketDTO createTicket(TicketDTO ticketDTO, MultipartFile attachment) {
        Ticket ticket = new Ticket();
        ticket.setTitle(ticketDTO.getTitle());
        ticket.setDescription(ticketDTO.getDescription());
        ticket.setCategory(ticketDTO.getCategory());
        ticket.setCreatedBy(AuthUtils.getAuthenticatedUserExactName());
        ticket.setTicketDepartment(ticketDTO.getTicketDepartment());

        Location location = locationRepository.findById(ticketDTO.getLocation())
                .orElseThrow(() -> new EntityNotFoundException("Location not found"));
        ticket.setLocation(location);

        User employeeUser = userRepository.findByEmployeeId(ticketDTO.getEmployee())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        ticket.setEmployee(employeeUser);

        assetRepository.findByAssetTag(ticketDTO.getAssetTag()).ifPresent(ticket::setAsset);

        User assignee = findExecutiveByLocationAndDepartment(location.getId(), ticketDTO.getTicketDepartment());
        ticket.setAssignee(assignee);

        ticket.setStatus(assignee == null ? TicketStatus.UNASSIGNED : TicketStatus.OPEN);

        List<String> ccEmails = new ArrayList<>();
        if (ticketDTO.getCcEmails() != null) ccEmails.addAll(ticketDTO.getCcEmails());
        if (assignee != null) ccEmails.add(assignee.getEmail());
        ticket.setCcEmails(ccEmails);

        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setDueDate(ticket.getCreatedAt().plusDays(3));

        // ✅ Save ticket first to get its ID
        Ticket savedTicket = ticketRepository.save(ticket);

        // ✅ Store the file if uploaded
        if (attachment != null && !attachment.isEmpty()) {
            String uploadDir = "uploads/tickets/";
            new File(uploadDir).mkdirs(); // Ensure directory exists

            String filename = savedTicket.getId() + "_" + attachment.getOriginalFilename();
            Path filePath = Paths.get(uploadDir, filename);

            try {
                Files.write(filePath, attachment.getBytes());
                savedTicket.setAttachmentPath(filePath.toString());
                ticketRepository.save(savedTicket); // Save path
            } catch (IOException e) {
                throw new RuntimeException("Failed to store attachment", e);
            }
        }

        // ✅ Send acknowledgment email
        emailTicketService.sendTicketAcknowledgmentEmail(
                employeeUser.getEmail(),
                savedTicket,
                ccEmails,
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


//    public List<TicketDTO> searchTickets(Long ticketId, String title, String category,
//                                         TicketStatus status, String employee, String assignee,
//                                         String assetTag, Long locationId) {
//
//        if (ticketId != null) {
//            return ticketRepository.findById(ticketId)
//                    .map(ticket -> List.of(convertTicketToDTO(ticket)))
//                    .orElse(List.of());
//        }
//
//        List<Ticket> tickets = ticketRepository.findAll();
//
//        return tickets.stream()
//                .filter(ticket -> title == null ||
//                        (ticket.getTitle() != null && ticket.getTitle().toLowerCase().contains(title.toLowerCase())))
//                .filter(ticket -> category == null ||
//                        (ticket.getCategory() != null && ticket.getCategory().name().equalsIgnoreCase(category)))
//                .filter(ticket -> status == null || ticket.getStatus() == status)
//                .filter(ticket -> employee == null ||
//                        (ticket.getEmployee() != null && ticket.getEmployee().getUsername().equalsIgnoreCase(employee)))
//                .filter(ticket -> assignee == null ||
//                        (ticket.getAssignee() != null && ticket.getAssignee().getUsername().equalsIgnoreCase(assignee)))
//                .filter(ticket -> assetTag == null ||
//                        (ticket.getAsset() != null && ticket.getAsset().getAssetTag().equalsIgnoreCase(assetTag)))
//                .filter(ticket -> locationId == null ||
//                        (ticket.getLocation() != null && ticket.getLocation().getId().equals(locationId)))
//                .map(this::convertTicketToDTO)
//                .collect(Collectors.toList());
//    }

//    public Page<TicketDTO> searchTickets(Long ticketId, String title, Pageable pageable) {
//        User user= userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
//                .orElseThrow((->UserNotFoundException));
//        Specification<Ticket> spec = TicketSpecification.getFilteredTickets(ticketId, title);
//        return ticketRepository.findAll(spec, pageable)
//                .map(this::convertTicketToDTO);
//    }

    public Page<TicketDTO> searchTickets(Long ticketId, String title, Pageable pageable) {
        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Specification<Ticket> spec;

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            spec = TicketSpecification.getFilteredTickets(ticketId, title); // unrestricted
        } else {
            spec = TicketSpecification.getFilteredTickets(ticketId, title, user); // restricted
        }

        return ticketRepository.findAll(spec, pageable)
                .map(this::convertTicketToDTO);
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
        emailTicketService.sendAcknowledgmentReplyToTicket
                (ticketId,message,ticket.getInternetMessageId());

        return convertMessageToDTO(savedMessage);
    }

    public void updateDueDate(Long ticketId, LocalDateTime newDueDate) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        LocalDateTime oldDueDate=ticket.getDueDate();
        User updater = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        ticket.setDueDate(newDueDate);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        LocalDateTime updatedDueDate=ticket.getDueDate();
        // Create a new TicketMessage entry for status update
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(updater); // The user updating the status
        message.setMessage("Due Date changed from " + oldDueDate + " to " + updatedDueDate);
        message.setSentAt(LocalDateTime.now());
        message.setStatusUpdatedBy(updater);

        ticketMessageRepository.save(message);

    }

    public void updateEmployeeIfSameAsAssignee(Long ticketId, String newEmployeeId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id " + ticketId));

//        if (ticket.getAssignee() != null && ticket.getEmployee() != null &&
//                ticket.getAssignee().getId().equals(ticket.getEmployee().getId())) {

            User newEmployee = userRepository.findByEmployeeId(newEmployeeId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id " + newEmployeeId));

            String oldEmployee=ticket.getEmployee().getUsername();
            ticket.setEmployee(newEmployee);
            ticket.setUpdatedAt(LocalDateTime.now());
             ticketRepository.save(ticket);
//        }
        User updater=userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(()-> new UserNotFoundException("Authenticated user not found "));

        // Create a new TicketMessage entry for status update
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(updater); // The user updating the status
        message.setMessage("Employee changed from " + oldEmployee + " to " + newEmployeeId);
        message.setSentAt(LocalDateTime.now());
        message.setStatusUpdatedBy(updater);

        ticketMessageRepository.save(message);

//        return ticket; // No change
    }

    public ResponseEntity<Resource> downloadAttachment(Long ticketId) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));

        if (ticket.getAttachmentPath() == null) {
            throw new FileNotFoundException("Attachment not found for this ticket");
        }

        Path path = Paths.get(ticket.getAttachmentPath());
        String fileName = path.getFileName().toString();
        String contentType = Files.probeContentType(path); // auto-detect content type

        Resource resource = new UrlResource(path.toUri());

//        System.out.println("Content-Disposition: attachment; filename=\"" + fileName + "\"");
//        System.out.println("Content-Type: " + contentType);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
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

    public PaginatedResponse<TicketDTO> getUserTickets(TicketStatus status, int page, int size) {
        String employeeId =AuthUtils.getAuthenticatedUsername();
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<Ticket> combinedTickets;

        if (status != null) {
            List<Ticket> employeeTickets = ticketRepository.findByEmployeeAndStatus(user, status);
            List<Ticket> assigneeTickets = ticketRepository.findByAssigneeAndStatus(user, status);
            combinedTickets = Stream.concat(employeeTickets.stream(), assigneeTickets.stream())
                    .distinct().toList();
        } else {
            List<Ticket> employeeTickets = ticketRepository.findByEmployee(user);
            List<Ticket> assigneeTickets = ticketRepository.findByAssignee(user);
            combinedTickets = Stream.concat(employeeTickets.stream(), assigneeTickets.stream())
                    .distinct().toList();
        }

        // Paginate manually since we combined two lists
        int start = Math.min(page * size, combinedTickets.size());
        int end = Math.min(start + size, combinedTickets.size());
        List<TicketDTO> paginatedList = combinedTickets.subList(start, end).stream()
                .map(this::convertTicketToDTO)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                paginatedList,
                page,
                size,
                combinedTickets.size(),
                (int) Math.ceil((double) combinedTickets.size() / size),
                end >= combinedTickets.size()
        );
    }

    public List<String> updateCcEmail(Long ticketId, TicketCcEmailUpdateRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with ID: " + ticketId));
        userRepository.findByEmail(request.getEmail()).orElseThrow(() ->
                new UserNotFoundException("User with email not found"));

        List<String> ccEmails = ticket.getCcEmails();

        if (request.isAdd()) {
            if (!ccEmails.contains(request.getEmail())) {
                ccEmails.add(request.getEmail());
            }
        } else {
            ccEmails.remove(request.getEmail());
        }

        ticket.setCcEmails(ccEmails);
        ticketRepository.save(ticket);

        return ccEmails;
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

    public List<UserIdNameDTO> getAllUserIdAndNames() {
        return locationAssignmentRepository.findAll()
                .stream()
                .map(la -> new UserIdNameDTO(
                        la.getItExecutive().getEmployeeId(), // Use employeeId instead of ID
                        la.getItExecutive().getUsername()
                ))
                .distinct()
                .collect(Collectors.toList());
    }

//    private TicketDTO convertTicketToDTO(Ticket ticket) {
//        List<TicketMessageDTO> messages = ticketMessageRepository.findByTicket(ticket)
//                .stream()
//                .map(this::convertMessageToDTO)
//                .collect(Collectors.toList());
//
//
//        return new TicketDTO(
//                ticket.getId(),
//                ticket.getTitle(),
//                ticket.getDescription(),
//                ticket.getCategory(),
//
//                ticket.getStatus(),
//                ticket.getEmployee() != null ? ticket.getEmployee().getUsername() : null, // ✅ Employee ID instead of username
//                ticket.getCreatedBy(),
//                ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : null, // ✅ Assignee Username
//                ticket.getAsset() != null ? ticket.getAsset().getAssetTag() : null, // ✅ Asset ID
//                ticket.getAsset() != null ? ticket.getAsset().getName() : "Other", // ✅ Asset Name or "Other"
//                ticket.getLocation() != null ? ticket.getLocation().getName():null,
//                ticket.getLocation() != null ? ticket.getLocation().getId():null,
//                ticket.getTicketDepartment(),
//                ticket.getCcEmails(),
//                ticket.getCreatedAt(),
//                ticket.getUpdatedAt(),
//                messages
//
//        );
//    }

    private TicketDTO convertTicketToDTO(Ticket ticket) {
        List<TicketMessage> messageEntities = ticketMessageRepository.findByTicket(ticket);

        List<TicketMessageDTO> messages = messageEntities
                .stream()
                .map(this::convertMessageToDTO)
                .collect(Collectors.toList());

        // 1. First Responded At — first message by someone other than ticket creator
        LocalDateTime firstRespondedAt = null;

        if (ticket.getAssignee() != null) {
            firstRespondedAt = messageEntities.stream()
                    .filter(msg -> msg.getSender() != null
                            && msg.getSender().getId().equals(ticket.getAssignee().getId()))
                    .map(TicketMessage::getSentAt)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
        }


        // 2. Last Updated — latest message or ticket updatedAt
        LocalDateTime lastUpdated = Stream.concat(
                        messageEntities.stream().map(TicketMessage::getSentAt),
                        Stream.of(ticket.getUpdatedAt())
                ).filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(ticket.getUpdatedAt());

        // 3. Due Date — example: 3 business days after ticket creation
        LocalDateTime dueDate = ticket.getCreatedAt() != null
                ? ticket.getCreatedAt().plusDays(3)
                : null;

        // 4. Closed At — only if ticket is closed
        LocalDateTime closedAt = (ticket.getStatus() == TicketStatus.CLOSED)
                ? ticket.getUpdatedAt()
                : null;

        // Build DTO
        TicketDTO dto = new TicketDTO(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getCategory(),
                ticket.getStatus(),
                ticket.getEmployee() != null ? ticket.getEmployee().getUsername() : null,
                ticket.getCreatedBy(),
                ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : null,
                ticket.getAsset() != null ? ticket.getAsset().getAssetTag() : null,
                ticket.getAsset() != null ? ticket.getAsset().getName() : "Other",
                ticket.getLocation() != null ? ticket.getLocation().getName() : null,
                ticket.getLocation() != null ? ticket.getLocation().getId() : null,
                ticket.getTicketDepartment(),
                ticket.getCcEmails(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                messages,
                firstRespondedAt,
                lastUpdated,
                dueDate,
                closedAt,
                ticket.getAttachmentPath()
        );

        return dto;
    }

    public TicketMessageDTO convertMessageToDTO(TicketMessage ticketMessage) {
        return new TicketMessageDTO(
                ticketMessage.getId(),
                ticketMessage.getMessage(),
                ticketMessage.getSender().getUsername(), // ✅ Convert User to username
                ticketMessage.getSentAt()
        );
    }

    // data visulation part methods





    public Map<TicketStatus, Long> getTicketCountByStatus() {
        return Arrays.stream(TicketStatus.values())
                .collect(Collectors.toMap(status -> status, status -> ticketRepository.countByStatus(status)));
    }

    public Map<String, Long> getTicketsCreatedPerDay() {
        List<Ticket> tickets = ticketRepository.findAll();
        Map<String, Long> stats = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (Ticket ticket : tickets) {
            String date = ticket.getCreatedAt().format(formatter);
            stats.put(date, stats.getOrDefault(date, 0L) + 1);
        }

        return stats;
    }

    public Map<String, Long> getTicketCountByCategory() {
        List<Ticket> tickets = ticketRepository.findAll();
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory().name() : "Uncategorized",
                        Collectors.counting()
                ));
    }

    public Map<String, Long> getTicketCountByAssignee() {
        List<Ticket> tickets = ticketRepository.findAll();
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getAssignee() != null ? t.getAssignee().getUsername() : "Unassigned",
                        Collectors.counting()
                ));
    }

    public ResolutionTimeStatsDTO getResolutionTimeStats() {
        List<Ticket> tickets = ticketRepository.findAll();

        List<Long> resolutionTimes = tickets.stream()
                .filter(t -> t.getCreatedAt() != null && t.getUpdatedAt() != null &&
                        (t.getStatus() == TicketStatus.RESOLVED || t.getStatus() == TicketStatus.CLOSED))
                .map(t -> java.time.Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes())
                .collect(Collectors.toList());

        long avg = (long) resolutionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long min = resolutionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = resolutionTimes.stream().mapToLong(Long::longValue).max().orElse(0);

        return new ResolutionTimeStatsDTO(avg, min, max);
    }

//    public ResolutionTimeStatsDTO getAssigneeResolutionStats(String assigneeId) {
//        User user = userRepository.findByEmployeeId(assigneeId)
//                .orElseThrow(() -> new UserNotFoundException("User not found with employee ID: " + assigneeId));
//
//        List<Ticket> tickets = ticketRepository.findByAssignee(user);
//
//        List<Long> resolutionTimes = tickets.stream()
//                .filter(t -> t.getCreatedAt() != null && t.getUpdatedAt() != null &&
//                        (t.getStatus() == TicketStatus.RESOLVED || t.getStatus() == TicketStatus.CLOSED))
//                .map(t -> java.time.Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes())
//                .collect(Collectors.toList());
//
//        long avg = (long) resolutionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
//        long min = resolutionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
//        long max = resolutionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
//
//        return new ResolutionTimeStatsDTO(avg, min, max);
//    }

    public ResolutionTimeStatsDTO getAssigneeResolutionStats(String assigneeId) {
        User user = userRepository.findByEmployeeId(assigneeId)
                .orElseThrow(() -> new UserNotFoundException("User not found with employee ID: " + assigneeId));

        List<Ticket> tickets = ticketRepository.findByAssignee(user);

        List<Long> resolutionTimesInMinutes = tickets.stream()
                .filter(t -> t.getCreatedAt() != null && t.getUpdatedAt() != null && t.getStatus() == TicketStatus.CLOSED)
                .map(t -> Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes())
                .collect(Collectors.toList());

        long avg = (long) resolutionTimesInMinutes.stream().mapToLong(Long::longValue).average().orElse(0);
        long min = resolutionTimesInMinutes.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = resolutionTimesInMinutes.stream().mapToLong(Long::longValue).max().orElse(0);

        return new ResolutionTimeStatsDTO(avg, min, max); // still in minutes
    }


    public List<TicketStatusTimeSeriesDTO> getTicketStatusOverTime() {

        List<Ticket> tickets = ticketRepository.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Map<TicketStatus, Long>> dailyStatusMap = new TreeMap<>();

        for (Ticket ticket : tickets) {
            if (ticket.getCreatedAt() != null && ticket.getStatus() != null) {
                String date = ticket.getCreatedAt().format(formatter);
                dailyStatusMap.putIfAbsent(date, new EnumMap<>(TicketStatus.class));
                Map<TicketStatus, Long> statusMap = dailyStatusMap.get(date);
                statusMap.put(ticket.getStatus(), statusMap.getOrDefault(ticket.getStatus(), 0L) + 1);
            }
        }

        List<TicketStatusTimeSeriesDTO> series = new ArrayList<>();
        for (Map.Entry<String, Map<TicketStatus, Long>> entry : dailyStatusMap.entrySet()) {
            series.add(new TicketStatusTimeSeriesDTO(entry.getKey(), entry.getValue()));
        }

        return series;
    }

    public Map<String, Long> getTopTicketReporters() {
        List<Ticket> tickets = ticketRepository.findAll();
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getEmployee() != null ? t.getEmployee().getUsername() : "Unknown",
                        Collectors.counting()
                ));
    }
}
