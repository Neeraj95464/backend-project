package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.*;
import AssetManagement.AssetManagement.enums.*;
import AssetManagement.AssetManagement.exception.UserNotFoundException;
import AssetManagement.AssetManagement.mapper.TicketMapper;
import AssetManagement.AssetManagement.repository.*;
import AssetManagement.AssetManagement.util.AuthUtils;
import AssetManagement.AssetManagement.util.TicketSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final UserRepository userRepository;
    private final UserService userService;
//    private final TicketFeedbackRepository ticketFeedbackRepository;
    private final TicketFeedbackRepository ticketFeedbackRepository;
    private final AssetRepository assetRepository;
    private final LocationAssignmentRepository locationAssignmentRepository;
    private final TicketMapper ticketMapper;
    private final LocationRepository locationRepository;
    private final EmailService emailTicketService;

    public TicketService(TicketRepository ticketRepository, TicketMessageRepository ticketMessageRepository, UserRepository userRepository, UserService userService, TicketFeedbackRepository ticketFeedbackRepository, AssetRepository assetRepository, LocationAssignmentRepository locationAssignmentRepository, TicketMapper ticketMapper, LocationRepository locationRepository, EmailService emailTicketService, EmailService emailTicketService1) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.ticketFeedbackRepository = ticketFeedbackRepository;
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
//        ticket.setTicketDepartment(ticketDTO.getTicketDepartment());

        TicketCategory category = ticketDTO.getCategory();

        if (category == TicketCategory.CCTV || category == TicketCategory.UPS) {
            String deptName = category.name();
//            System.out.println("Setting department from category: " + deptName);
            ticket.setTicketDepartment(TicketDepartment.valueOf(deptName));
        } else if (category == TicketCategory.FOCUS ) {
            String deptName = category.name();
//            System.out.println("Setting department from category: " + deptName);
            ticket.setTicketDepartment(TicketDepartment.valueOf(deptName));
        }
        else {
//            System.out.println("Setting department from DTO: " + ticketDTO.getTicketDepartment());
            ticket.setTicketDepartment(ticketDTO.getTicketDepartment());
        }

        Location location = locationRepository.findById(ticketDTO.getLocation())
                .orElseThrow(() -> new EntityNotFoundException("Location not found"));

        ticket.setLocation(location);

        User employeeUser = userRepository.findByEmployeeId(ticketDTO.getEmployee().getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        ticket.setEmployee(employeeUser);

        assetRepository.findByAssetTag(ticketDTO.getAssetTag()).ifPresent(ticket::setAsset);

        User assignee = findExecutiveByLocationAndDepartment(location.getId(), ticket.getTicketDepartment());
        ticket.setAssignee(assignee);

        User locationManager = locationAssignmentRepository.findLocationManagerByLocationAndTicketDepartment
                (location,ticket.getTicketDepartment());

        ticket.setStatus(assignee == null ? TicketStatus.UNASSIGNED : TicketStatus.OPEN);

        List<String> ccEmails = new ArrayList<>();
        if (ticketDTO.getCcEmails() != null) ccEmails.addAll(ticketDTO.getCcEmails());
        if (assignee != null) ccEmails.add(assignee.getEmail());
        if (locationManager != null) ccEmails.add(locationManager.getEmail());
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

    public ResponseEntity<List<TicketDTO>> createBulkTicket(List<Ticket> ticketList, MultipartFile attachment) {
        // Save all tickets
        List<Ticket> savedTickets = ticketRepository.saveAll(ticketList);

        // Optionally handle attachment here for each ticket or store globally (e.g., save to S3, DB, etc.)

        // Convert to DTOs
        List<TicketDTO> ticketDTOs = savedTickets.stream()
                .map(ticketMapper::toDTO)
                .collect(Collectors.toList());

        return new ResponseEntity<>(ticketDTOs, HttpStatus.CREATED);
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

    public TicketDTO updateLocation(Long ticketId, Long locationId) {
        User updater = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(()->new UserNotFoundException("Authenticated User not found"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));

        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Location not found"));

        String oldLocation = ticket.getLocation() != null ? ticket.getLocation().getName() : null;

        ticket.setLocation(location);
        Ticket savedTicket = ticketRepository.save(ticket);

        TicketMessage message = new TicketMessage();
        message.setTicket(savedTicket);
        message.setSender(updater); // The user updating the status
        message.setMessage("Ticket Location changed from " + oldLocation + " to " + savedTicket.getLocation().getName());
        message.setSentAt(LocalDateTime.now());
        message.setStatusUpdatedBy(updater);
        message.setTicketMessageType(TicketMessageType.PUBLIC_RESPONSE);

        ticketMessageRepository.save(message);

        return convertTicketToDTO(savedTicket);
    }

    public TicketDTO updateCategory(Long ticketId, TicketCategory category) {
        User updater = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(()->new UserNotFoundException("Authenticated User not found"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));

//        System.out.println("came here 1");

        String oldCategory = ticket.getCategory() != null ? ticket.getCategory().name() : null;

        ticket.setCategory(category);
        Ticket savedTicket = ticketRepository.save(ticket);

//        System.out.println("came here 2");

        TicketMessage message = new TicketMessage();
        message.setTicket(savedTicket);
        message.setSender(updater); // The user updating the status
        message.setMessage("Ticket Category changed from " + oldCategory + " to " + savedTicket.getCategory().name());
        message.setSentAt(LocalDateTime.now());
        message.setStatusUpdatedBy(updater);
        message.setTicketMessageType(TicketMessageType.PUBLIC_RESPONSE);

        ticketMessageRepository.save(message);

        return convertTicketToDTO(savedTicket);
    }


    public PaginatedResponse<TicketDTO> getAllTicketsForAdmin(int page, int size, TicketStatus status) {
        // Fetch the authenticated user
        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("Authenticated User not found"));

        String role = user.getRole();

        if (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("HR_ADMIN")) {
            throw new RuntimeException("Access Denied: Only admins and HR admins can view all tickets.");
        }

        TicketDepartment userDepartment = TicketDepartment.valueOf(user.getDepartment().name()); // Assuming Department is an enum

        // Define page request sorted by createdAt in descending order
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Ticket> ticketPage;

        // Apply status + department filtering
        if (status != null && "UNASSIGNED".equalsIgnoreCase(status.name())) {
            // Fetch unassigned tickets (assignee is null)
            ticketPage = ticketRepository.findByStatus(status,pageRequest);
        } else if (status == null || "ALL".equalsIgnoreCase(String.valueOf(status))) {
            // Fetch tickets filtered by department only
            ticketPage = ticketRepository.findByTicketDepartment(userDepartment, pageRequest);
        }
         else {
            // Fetch tickets filtered by both department and status
            ticketPage = ticketRepository.findByTicketDepartmentAndStatus(userDepartment, status, pageRequest);
        }

        List<TicketDTO> ticketDTOs = ticketPage.getContent().stream()
                .map(ticketMapper::toDTO)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                ticketDTOs,
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages(),
                ticketPage.isLast()
        );
    }

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

        User updater= userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("user not found with to update assignee"));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        User oldTicketAssignee = ticket.getAssignee() != null
                ? ticket.getAssignee()
                : new User(); // creates an empty User object if assignee is null


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

        TicketMessage message = new TicketMessage();
        message.setTicket(updatedTicket);
        message.setSender(updater); // The user updating the status
//        assert oldTicketAssignee != null;
        message.setMessage("Ticket assignee changed from " + oldTicketAssignee.getUsername() + " to " + updatedTicket.getAssignee().getUsername());
        message.setSentAt(LocalDateTime.now());
        message.setStatusUpdatedBy(updater);
        message.setTicketMessageType(TicketMessageType.PUBLIC_RESPONSE);

        ticketMessageRepository.save(message);


        return convertTicketToDTO(updatedTicket);
    }

    @Transactional
    public TicketMessageDTO addMessage(Long ticketId, TicketMessageDTO ticketMessageDTO) {
        String employeeId = AuthUtils.getAuthenticatedUsername();
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        User senderUser = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        TicketMessage ticketMessage = new TicketMessage();
        ticketMessage.setTicket(ticket);
        ticketMessage.setMessage(ticketMessageDTO.getMessage());
        ticketMessage.setSender(senderUser);
        ticketMessage.setSentAt(LocalDateTime.now());
        ticketMessage.setTicketMessageType(ticketMessageDTO.getTicketMessageType());

        TicketMessage savedMessage = ticketMessageRepository.save(ticketMessage);

        // modify the rule of send mail to internal
        if(savedMessage.getTicketMessageType()== TicketMessageType.INTERNAL_NOTE){
            emailTicketService.sendInternalMail(senderUser.getEmail(),savedMessage.getMessage()
            ,ticket.getCcEmails(),ticket);

        }else {
            emailTicketService.sendAcknowledgmentReplyToTicket
                    (ticketId,savedMessage.getMessage(),ticket.getInternetMessageId());
        }

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
        message.setTicketMessageType(TicketMessageType.PUBLIC_RESPONSE);

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
        message.setTicketMessageType(TicketMessageType.PUBLIC_RESPONSE);

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


        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }


    public List<LocationAssignmentDTO> getAllAssignments() {
        User currentUser = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        TicketDepartment userDept = TicketDepartment.valueOf(currentUser.getDepartment().name());
        boolean isHR = userDept == TicketDepartment.HR;

        List<Location> locations = locationRepository.findAll();
        List<TicketDepartment> departments = Arrays.asList(TicketDepartment.values());

        List<LocationAssignmentDTO> result = new ArrayList<>();

        for (Location location : locations) {
            for (TicketDepartment department : departments) {

                // Skip HR department if user is not HR
                if (!isHR && department == TicketDepartment.HR) {
                    continue;
                }

                // Skip non-HR departments if user is HR
                if (isHR && department != TicketDepartment.HR) {
                    continue;
                }

                Optional<LocationAssignment> assignmentOpt = locationAssignmentRepository
                        .findByLocationAndTicketDepartment(location, department);

                String executiveUsername = "null";
                String managerUsername = "null";

                if (assignmentOpt.isPresent()) {
                    LocationAssignment assignment = assignmentOpt.get();
                    if (assignment.getItExecutive() != null) {
                        executiveUsername = Optional.ofNullable(assignment.getItExecutive().getUsername()).orElse("null");
                    }
                    if (assignment.getLocationManager() != null) {
                        managerUsername = Optional.ofNullable(assignment.getLocationManager().getUsername()).orElse("null");
                    }
                }

                result.add(new LocationAssignmentDTO(
                        location.getId(),
                        location.getName(),
                        department,
                        executiveUsername,
                        managerUsername
                ));
            }
        }

        return result;
    }



    @Transactional
    public ResponseEntity<LocationAssignment> assignLocation(LocationAssignmentRequest request) {
//        System.out.println("Request was " + request);

        User executive = userRepository.findByEmployeeId(request.getExecutiveEmployeeId())
                .orElseThrow(() -> new RuntimeException("Executive not found"));

        User manager = userRepository.findByEmployeeId(request.getManagerEmployeeId())
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new RuntimeException("Location not found"));

        // Delete existing assignment for that location and department
        locationAssignmentRepository.findByLocationAndTicketDepartment(location, request.getTicketDepartment())
                .ifPresent(existing -> {
                    locationAssignmentRepository.delete(existing);
                    locationAssignmentRepository.flush(); // Force immediate execution of DELETE
//                    System.out.println("Deleted old assignment with ID = " + existing.getId());
                });

        // Save new assignment
        LocationAssignment assignment = new LocationAssignment();
        assignment.setItExecutive(executive);
        assignment.setLocationManager(manager);
        assignment.setLocation(location);
        assignment.setTicketDepartment(request.getTicketDepartment());

        LocationAssignment savedAssignment = locationAssignmentRepository.save(assignment);
//        System.out.println("Saved Assignment ID = " + savedAssignment.getId());

        return ResponseEntity.ok(savedAssignment);
    }

    // TicketService.java
//    public List<AssigneeFeedbackDTO> getFeedbackGroupedByAssignee() {
//        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername()).orElseThrow(
//                ()->new UserNotFoundException("Authenticated user not found ")
//        );
//        TicketDepartment ticketDepartment = TicketDepartment.valueOf(user.getDepartment().name());
//
//        return ticketFeedbackRepository.getFeedbackGroupedByAssignee(ticketDepartment);
//    }

    public List<AssigneeFeedbackStatsDTO> getFeedbackGroupedByAssignee() {
        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        boolean isHrUser = user.getDepartment().name().equals("HR");

        return ticketFeedbackRepository.getFeedbackGroupedByAssigneeStats(isHrUser);
    }


    public Page<TicketDTO> getTicketsBySiteWithDate(Long siteId, TicketStatus status, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }

        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername()).orElseThrow(
                ()->new UserNotFoundException("Authenticated user not found ")
        );
        TicketDepartment ticketDepartment = TicketDepartment.valueOf(user.getDepartment().name());

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Ticket> tickets = ticketRepository.findByLocation_Site_IdAndStatusAndCreatedAtBetween(
                siteId, ticketDepartment, status, startDate, endDate, pageable);


        return tickets.map(this::convertTicketToDTO);
    }

    public ResponseEntity<String> updateFeedback(Long ticketId, int rating) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        Optional<TicketFeedback> existingFeedback = ticketFeedbackRepository.findByTicket(ticket);

        if (existingFeedback.isPresent()) {
            return ResponseEntity.ok("<html><body><h2>You have already submitted feedback 🙏</h2></body></html>");
        }

        // Save rating immediately
        TicketFeedback feedback = new TicketFeedback();
        feedback.setTicket(ticket);
        feedback.setRating(rating);
        feedback.setSubmittedAt(LocalDateTime.now());
        ticketFeedbackRepository.save(feedback);

        // If rating is 1 or 5, ask for message
        if (rating == 1 || rating == 5) {
            return ResponseEntity.ok("""
            <html>
            <body>
                <h2>Thanks for rating!</h2>
                <p>We’d really appreciate it if you could tell us more:</p>
                <form method='post' action='https://numerous-gem-accompanied-mac.trycloudflare.com/api/feedback/message'>
                    <input type='hidden' name='ticketId' value='%d'/>
                    <textarea name='message' placeholder='Your feedback' required></textarea><br/>
                    <button type='submit'>Submit Message</button>
                </form>
            </body>
            </html>
        """.formatted(ticketId));
        }

        // For ratings 2–4, just thank them
        return ResponseEntity.ok("<html><body><h2>Thank you for your feedback! ⭐</h2></body></html>");
    }


    public String generateFeedbackEmailHtml(Long ticketId) {
        String feedbackApiUrl = "https://numerous-gem-accompanied-mac.trycloudflare.com/api/feedback"; // Update with real domain in prod

        return """
        <html>
        <body style="font-family: Arial, sans-serif; line-height: 1.6;">
            <p>Hello,</p>
            <p>Your support ticket <strong>#%d</strong> has been resolved.</p>
            <p>We would love to hear your feedback!</p>
            <p>Please click a star below to rate your experience:</p>

            <p style="font-size: 24px;">
                <a href="%s?ticketId=%d&rating=1" title="Very Dissatisfied">⭐</a>
                <a href="%s?ticketId=%d&rating=2" title="Dissatisfied">⭐</a>
                <a href="%s?ticketId=%d&rating=3" title="Neutral">⭐</a>
                <a href="%s?ticketId=%d&rating=4" title="Satisfied">⭐</a>
                <a href="%s?ticketId=%d&rating=5" title="Very Satisfied">⭐</a>
            </p>

            <p>Thank you!<br><strong>IT Support Team</strong></p>
        </body>
        </html>
    """.formatted(
                ticketId,
                feedbackApiUrl, ticketId,
                feedbackApiUrl, ticketId,
                feedbackApiUrl, ticketId,
                feedbackApiUrl, ticketId,
                feedbackApiUrl, ticketId
        );
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
        Ticket savedTicket = ticketRepository.save(ticket);

        // Create a new TicketMessage entry for status update
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(updater); // The user updating the status
        message.setMessage("Status changed from " + oldStatus + " to " + newStatus);
        message.setSentAt(LocalDateTime.now());
        message.setStatusUpdate(newStatus);
        message.setStatusUpdatedBy(updater);
        message.setTicketMessageType(TicketMessageType.PUBLIC_RESPONSE);

        ticketMessageRepository.save(message);

        if (!savedTicket.getAssignee().equals(savedTicket.getEmployee())) {
            String toEmail = ticket.getEmployee().getEmail();
            String subject = "Ticket ID: " + ticket.getId() + " - " +
                    (savedTicket.getTitle() != null ? savedTicket.getTitle() : ticket.getTitle());
            String feedbackHtml = generateFeedbackEmailHtml(ticket.getId());

            emailTicketService.sendEmailViaGraph(toEmail, subject, feedbackHtml, null);
        }

        return convertTicketToDTO(ticket);
    }



//    public PaginatedResponse<TicketDTO> getUserTickets(TicketStatus status, String employeeId, int page, int size) {
//        User user;
//
//        // Determine the user context
//        if ("ALL".equalsIgnoreCase(employeeId)) {
//            String authenticatedEmployeeId = AuthUtils.getAuthenticatedUsername();
//            user = userRepository.findByEmployeeId(authenticatedEmployeeId)
//                    .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
//        } else {
//            user = userRepository.findByEmployeeId(employeeId)
//                    .orElseThrow(() -> new UserNotFoundException("Specified user not found"));
//        }
//
//        List<Ticket> combinedTickets;
//
//        if (status != null) {
//            List<Ticket> employeeTickets = ticketRepository.findByEmployeeAndStatus(user, status);
//            List<Ticket> assigneeTickets = ticketRepository.findByAssigneeAndStatus(user, status);
//            combinedTickets = Stream.concat(employeeTickets.stream(), assigneeTickets.stream())
//                    .distinct().toList();
//        } else {
//            List<Ticket> employeeTickets = ticketRepository.findByEmployee(user);
//            List<Ticket> assigneeTickets = ticketRepository.findByAssignee(user);
//            combinedTickets = Stream.concat(employeeTickets.stream(), assigneeTickets.stream())
//                    .distinct().toList();
//        }
//
//        // Manual pagination
//        int start = Math.min(page * size, combinedTickets.size());
//        int end = Math.min(start + size, combinedTickets.size());
//        List<TicketDTO> paginatedList = combinedTickets.subList(start, end).stream()
//                .map(this::convertTicketToDTO)
//                .collect(Collectors.toList());
//
//        return new PaginatedResponse<>(
//                paginatedList,
//                page,
//                size,
//                combinedTickets.size(),
//                (int) Math.ceil((double) combinedTickets.size() / size),
//                end >= combinedTickets.size()
//        );
//    }

    public PaginatedResponse<TicketDTO> getUserTickets(TicketStatus status, String employeeId, int page, int size) {
        User user;

        // Determine the user context
        if ("ALL".equalsIgnoreCase(employeeId)) {
            String authenticatedEmployeeId = AuthUtils.getAuthenticatedUsername();
            user = userRepository.findByEmployeeId(authenticatedEmployeeId)
                    .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
        } else {
            user = userRepository.findByEmployeeId(employeeId)
                    .orElseThrow(() -> new UserNotFoundException("Specified user not found"));
        }

        List<Ticket> combinedTickets;

        if (status != null) {
            List<Ticket> employeeTickets = ticketRepository.findByEmployeeAndStatus(user, status);
            List<Ticket> assigneeTickets = ticketRepository.findByAssigneeAndStatus(user, status);
            combinedTickets = Stream.concat(employeeTickets.stream(), assigneeTickets.stream())
                    .distinct()
                    .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed()) // Sort by createdAt descending
                    .toList();
        } else {
            List<Ticket> employeeTickets = ticketRepository.findByEmployee(user);
            List<Ticket> assigneeTickets = ticketRepository.findByAssignee(user);
            combinedTickets = Stream.concat(employeeTickets.stream(), assigneeTickets.stream())
                    .distinct()
                    .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed()) // Sort by createdAt descending
                    .toList();
        }

        // Manual pagination
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




    public List<TicketDTO> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(this::convertTicketToDTO)
                .collect(Collectors.toList());
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


//    public List<UserIdNameDTO> getAllUserIdAndNames() {
//        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
//                .orElseThrow(()->new UserNotFoundException("Authenticated User not found"));
//        TicketDepartment ticketDepartment= TicketDepartment.valueOf(user.getDepartment().name());
//
//        return locationAssignmentRepository.findAll()
//                .stream()
//                .flatMap(la -> Stream.of(
//                        new UserIdNameDTO(la.getItExecutive().getEmployeeId(), la.getItExecutive().getUsername()),
//                        new UserIdNameDTO(la.getLocationManager().getEmployeeId(), la.getLocationManager().getUsername())
//                ))
//                .distinct() // This will eliminate duplicate users based on equals/hashCode in DTO
//                .collect(Collectors.toList());
//    }


    public List<UserIdNameDTO> getAllUserIdAndNames() {
        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("Authenticated User not found"));

        TicketDepartment userDepartment = TicketDepartment.valueOf(user.getDepartment().name());

        List<LocationAssignment> assignments;

        if (userDepartment == TicketDepartment.HR) {
            // Get only HR-related assignments
            assignments = locationAssignmentRepository.findByTicketDepartment(TicketDepartment.HR);
        } else {
            // Get all assignments EXCEPT HR
            assignments = locationAssignmentRepository.findByTicketDepartmentNot(TicketDepartment.HR);
        }

        return assignments.stream()
                .flatMap(la -> Stream.of(
                        new UserIdNameDTO(la.getItExecutive().getEmployeeId(), la.getItExecutive().getUsername()),
                        new UserIdNameDTO(la.getLocationManager().getEmployeeId(), la.getLocationManager().getUsername())
                ))
                .distinct()
                .collect(Collectors.toList());
    }

//    public List<LocationAssignmentDTOResponse> getAllLocationDepartmentAssignments() {
//        List<Location> allLocations = locationRepository.findAll();
//        List<TicketDepartment> departments = Arrays.asList(TicketDepartment.values());
//
//        List<LocationAssignmentDTOResponse> result = new ArrayList<>();
//
//        for (Location location : allLocations) {
//            for (TicketDepartment department : departments) {
//                Optional<LocationAssignment> assignmentOpt = locationAssignmentRepository
//                        .findByLocationAndTicketDepartment(location, department);
//
//                LocationAssignmentDTOResponse dto = new LocationAssignmentDTOResponse();
//                dto.setLocation(location.getName());
//                dto.setDepartment(department);
//
//                if (assignmentOpt.isPresent()) {
//                    LocationAssignment assignment = assignmentOpt.get();
//                    dto.setItExecutive(new UserIdNameDTO(
//                            assignment.getItExecutive().getEmployeeId(),
//                            assignment.getItExecutive().getUsername()
//                    ));
//                    dto.setLocationManager(new UserIdNameDTO(
//                            assignment.getLocationManager().getEmployeeId(),
//                            assignment.getLocationManager().getUsername()
//                    ));
//                } else {
//                    dto.setItExecutive(null);
//                    dto.setLocationManager(null);
//                }
//
//                result.add(dto);
//            }
//        }
//
//        return result;
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
                ticket.getEmployee() != null ? userService.convertUserToDto(ticket.getEmployee()) : null,
                ticket.getCreatedBy(),
                ticket.getAssignee() != null ? userService.convertUserToDto(ticket.getAssignee()) : null,
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
                ticketMessage.getSentAt(),
                ticketMessage.getTicketMessageType()
        );
    }

    // data visulation part methods





    public Map<TicketStatus, Long> getTicketCountByStatus() {
        return Arrays.stream(TicketStatus.values())
                .collect(Collectors.toMap(status -> status, status -> ticketRepository.countByStatus(status)));
    }

    public Map<String, Long> getTicketsCreatedPerDay() {
        Map<String, Long> stats = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = today.minusDays(29);
        LocalDateTime startOfDay = startDate.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

        // Pre-fill with zeros for all 30 days
        for (int i = 0; i < 30; i++) {
            String date = startDate.plusDays(i).format(formatter);
            stats.put(date, 0L);
        }

        List<Ticket> tickets = ticketRepository.findTicketsCreatedBetween(startOfDay, endOfToday);

        for (Ticket ticket : tickets) {
            if (ticket.getCreatedAt() != null) {
                String date = ticket.getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .withZoneSameInstant(zone)
                        .toLocalDate()
                        .format(formatter);
                stats.put(date, stats.get(date) + 1);
            }
        }

        return stats;
    }






    public Map<String, Long> getTicketCountByCategory() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = today.minusDays(29);
        LocalDateTime startOfDay = startDate.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);


        List<Ticket> tickets = ticketRepository.findTicketsCreatedBetween(startOfDay,endOfToday);
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory().name() : "Uncategorized",
                        Collectors.counting()
                ));
    }

//    public Map<String, Long> getTicketCountByAssignee() {
//        ZoneId zone = ZoneId.of("Asia/Kolkata");
//        LocalDate today = LocalDate.now(zone);
//        LocalDate startDate = today.minusDays(29);
//        LocalDateTime startOfDay = startDate.atStartOfDay();
//        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
//fff
//        List<Ticket> tickets = ticketRepository.findTicketsCreatedBetween(startOfDay,endOfToday);
//        return tickets.stream()
//                .collect(Collectors.groupingBy(
//                        t -> t.getAssignee() != null ? t.getAssignee().getUsername() : "Unassigned",
//                        Collectors.counting()
//                ));
//    }



//    public ResolutionTimeStatsDTO getResolutionTimeStats() {
//        List<Ticket> tickets = ticketRepository.findAll();
//
//        List<Long> resolutionTimes = tickets.stream()
//                .filter(t -> t.getCreatedAt() != null && t.getUpdatedAt() != null &&
//                        (t.getStatus() == TicketStatus.CLOSED))
//                .map(t -> java.time.Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes())
//                .collect(Collectors.toList());
//
//        long avg = (long) resolutionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
//        long min = resolutionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
//        long max = resolutionTimes.stream().mapToLong(Long::longValue).max().orElse(0);
//
//        return new ResolutionTimeStatsDTO(avg, min, max);
//    }


//    public ResolutionTimeStatsDTO getResolutionTimeStats() {
//        List<Ticket> tickets = ticketRepository.findAll();
//
//        List<Ticket> validTickets = tickets.stream()
//                .filter(t -> t.getCreatedAt() != null &&
//                        t.getUpdatedAt() != null &&
//                        t.getStatus() == TicketStatus.CLOSED &&
//                        (t.getTicketDepartment() == null || t.getTicketDepartment() != TicketDepartment.HR))
//                .collect(Collectors.toList());
//
//        Map<String, List<Long>> weekly = new HashMap<>();
//        Map<String, List<Long>> monthly = new HashMap<>();
//        List<Long> overall = new ArrayList<>();
//
//        for (Ticket t : validTickets) {
//            long minutes = Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes();
//            overall.add(minutes);
//
////            String weekKey = t.getCreatedAt().getYear() + "-W" + t.getCreatedAt().get(ChronoField.ALIGNED_WEEK_OF_YEAR);
//            String monthAbbrev = t.getCreatedAt().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH); // e.g. "Jul"
//            int weekOfMonth = t.getCreatedAt().get(ChronoField.ALIGNED_WEEK_OF_MONTH); // e.g. 1, 2, 3...
//            String weekKey = monthAbbrev + " W" + weekOfMonth; // e.g. "Jul W2"
//
//            weekly.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(minutes);
//
//            String monthKey = t.getCreatedAt().getYear() + "-" + String.format("%02d", t.getCreatedAt().getMonthValue());
//            monthly.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(minutes);
//        }
//
//        Map<String, ResolutionStats> limitedWeeklyStats = limitToMostRecentWeeks(weekly, 5);
//        Map<String, ResolutionStats> limitedMonthlyStats = limitToMostRecentMonths(monthly, 4);
//
//        return new ResolutionTimeStatsDTO(
//                calculateStats(overall),
//                limitedWeeklyStats,
//                limitedMonthlyStats
//        );
//    }


    public ResolutionTimeStatsDTO getResolutionTimeStats() {
        List<Ticket> tickets = ticketRepository.findAll();

        // Only closed, non-HR, with valid timestamps
        List<Ticket> validTickets = tickets.stream()
                .filter(t -> t.getCreatedAt() != null
                        && t.getUpdatedAt() != null
                        && t.getStatus() == TicketStatus.CLOSED
                        && (t.getTicketDepartment() == null || t.getTicketDepartment() != TicketDepartment.HR)
                )
                .collect(Collectors.toList());

        // -- Prepare grouping --
        Map<String, List<Long>> weekly = new LinkedHashMap<>();
        Map<String, List<Long>> monthly = new LinkedHashMap<>();
        List<Long> overall = new ArrayList<>();

        // For current month/week stats
        LocalDate today = LocalDate.now();
        int thisMonth = today.getMonthValue();
        int thisYear = today.getYear();

        // For 3 month window (including current)
        LocalDate firstOfCurrentMonth = today.withDayOfMonth(1);
        LocalDate firstOf3rdLastMonth = firstOfCurrentMonth.minusMonths(2);

        for (Ticket t : validTickets) {
            LocalDateTime created = t.getCreatedAt();
            long minutes = Duration.between(created, t.getUpdatedAt()).toMinutes();
            overall.add(minutes);

            // --- Weekly -- only current month, week starts at 1
            if (created.getYear() == thisYear && created.getMonthValue() == thisMonth) {
                String monthAbbrev = created.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                int weekOfMonth = ((created.getDayOfMonth() - 1) / 7) + 1; // weeks start at 1
                String weekKey = monthAbbrev + " W" + weekOfMonth;
                weekly.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(minutes);
            }

            // --- Monthly: last 3 months (including current)
            if (!created.toLocalDate().isBefore(firstOf3rdLastMonth)) {
                String monthKey = created.getYear() + "-" + String.format("%02d", created.getMonthValue());
                monthly.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(minutes);
            }
        }

        // --- Pad weeks: Always show all weeks up to this week in current month
        int lastWeekIndex = ((today.getDayOfMonth() - 1) / 7) + 1;
        String thisMonthAbbrev = today.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        for (int i = 1; i <= lastWeekIndex; i++) {
            String wkKey = thisMonthAbbrev + " W" + i;
            weekly.putIfAbsent(wkKey, new ArrayList<>());
        }

        // --- Prepare output using improved stats with ticket count ---
        Map<String, ResolutionStats> weeklyStats = weekly.entrySet().stream()
                .sorted(Comparator.comparing(e -> Integer.parseInt(e.getKey().split(" W")[1])))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> calculateStats(e.getValue()),
                        (a, b) -> a, LinkedHashMap::new
                ));

        Map<String, ResolutionStats> monthlyStats = monthly.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .limit(3)
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> calculateStats(e.getValue()),
                        (a, b) -> a, LinkedHashMap::new
                ));

        return new ResolutionTimeStatsDTO(
                calculateStats(overall), // all closed tickets except HR
                weeklyStats,
                monthlyStats
        );
    }

    // Your improved stats method (in days, with count)
//    public ResolutionStats calculateStats(List<Long> times) {
//        int ticketCount = times.size();
//        if (ticketCount == 0) return new ResolutionStats(null, null, null, 0);
//
//        Double min = times.stream().mapToLong(Long::longValue).min().orElse(0) / 1440.0;
//        Double max = times.stream().mapToLong(Long::longValue).max().orElse(0) / 1440.0;
//        Double avg = times.stream().mapToLong(Long::longValue).average().orElse(0) / 1440.0;
//        return new ResolutionStats(avg, min, max, ticketCount);
//    }



//    public ResolutionTimeStatsDTO getAssigneeResolutionStats(String assigneeId) {
//        User user = userRepository.findByEmployeeId(assigneeId)
//                .orElseThrow(() -> new UserNotFoundException("User not found with employee ID: " + assigneeId));
//
//        List<Ticket> tickets = ticketRepository.findByAssignee(user);
//
//        List<Long> resolutionTimesInMinutes = tickets.stream()
//                .filter(t -> t.getCreatedAt() != null && t.getUpdatedAt() != null && t.getStatus() == TicketStatus.CLOSED)
//                .map(t -> Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes())
//                .collect(Collectors.toList());
//
//        long avg = (long) resolutionTimesInMinutes.stream().mapToLong(Long::longValue).average().orElse(0);
//        long min = resolutionTimesInMinutes.stream().mapToLong(Long::longValue).min().orElse(0);
//        long max = resolutionTimesInMinutes.stream().mapToLong(Long::longValue).max().orElse(0);
//
//        return new ResolutionTimeStatsDTO(avg, min, max); // still in minutes
//    }

//    public ResolutionTimeStatsDTO getAssigneeResolutionStats(String assigneeId) {
//        User user = userRepository.findByEmployeeId(assigneeId)
//                .orElseThrow(() -> new UserNotFoundException("User not found with employee ID: " + assigneeId));
//
//        List<Ticket> tickets = ticketRepository.findByAssignee(user);
//
//        List<Long> resolutionTimesInMinutes = tickets.stream()
//                .filter(t -> t.getCreatedAt() != null && t.getUpdatedAt() != null && t.getStatus() == TicketStatus.CLOSED)
//                .map(t -> Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes())
//                .collect(Collectors.toList());
//
//        if (resolutionTimesInMinutes.isEmpty()) {
//            return new ResolutionTimeStatsDTO(null, null, null); // or use -1 for all if nulls are not acceptable
//        }
//
//        long avg = (long) resolutionTimesInMinutes.stream().mapToLong(Long::longValue).average().orElse(0);
//        long min = resolutionTimesInMinutes.stream().mapToLong(Long::longValue).min().orElse(0);
//        long max = resolutionTimesInMinutes.stream().mapToLong(Long::longValue).max().orElse(0);
//
//        return new ResolutionTimeStatsDTO(avg, min, max);
//    }

//    public ResolutionTimeStatsDTO getAssigneeResolutionStats(String assigneeId) {
//        User user = userRepository.findByEmployeeId(assigneeId)
//                .orElseThrow(() -> new UserNotFoundException("User not found with employee ID: " + assigneeId));
//
//        List<Ticket> tickets = ticketRepository.findByAssignee(user);
//
//        // Only consider CLOSED tickets with valid timestamps
//        List<Ticket> closedTickets = tickets.stream()
//                .filter(t -> t.getStatus() == TicketStatus.CLOSED && t.getCreatedAt() != null && t.getUpdatedAt() != null)
//                .collect(Collectors.toList());
//
//        // Group by week
//        Map<String, List<Long>> weekly = new HashMap<>();
//        // Group by month
//        Map<String, List<Long>> monthly = new HashMap<>();
//        // Overall resolution times
//        List<Long> overall = new ArrayList<>();
//
//        for (Ticket t : closedTickets) {
//            long minutes = Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes();
//            overall.add(minutes);
//
//            // Weekly key: e.g., "2025-W29"
////            String weekKey = t.getCreatedAt().getYear() + "-W" + t.getCreatedAt().get(ChronoField.ALIGNED_WEEK_OF_YEAR);
//            String monthAbbrev = t.getCreatedAt().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH); // e.g. "Jul"
//            int weekOfMonth = t.getCreatedAt().get(ChronoField.ALIGNED_WEEK_OF_MONTH); // e.g. 1, 2, 3...
//            String weekKey = monthAbbrev + " W" + weekOfMonth; // e.g. "Jul W2"
//
//            weekly.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(minutes);
//
//            // Monthly key: e.g., "2025-07"
//            String monthKey = t.getCreatedAt().getYear() + "-" + String.format("%02d", t.getCreatedAt().getMonthValue());
//            monthly.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(minutes);
//        }
//
//        Map<String, ResolutionStats> limitedWeeklyStats = limitToMostRecentWeeks(weekly, 5);
//        Map<String, ResolutionStats> limitedMonthlyStats = limitToMostRecentMonths(monthly, 4);
//
//        return new ResolutionTimeStatsDTO(
//                calculateStats(overall),
//                limitedWeeklyStats,
//                limitedMonthlyStats
//        );
//    }
//
//    public ResolutionStats calculateStats(List<Long> times) {
//        if (times.isEmpty()) return new ResolutionStats(null, null, null);
//
//        long min = times.stream().mapToLong(Long::longValue).min().orElse(0);
//        long max = times.stream().mapToLong(Long::longValue).max().orElse(0);
//        long avg = (long) times.stream().mapToLong(Long::longValue).average().orElse(0);
//
//        return new ResolutionStats(avg, min, max);
//    }
//
//    public Map<String, ResolutionStats> calculateGroupedStats(Map<String, List<Long>> groupedTimes) {
//        return groupedTimes.entrySet().stream()
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        e -> calculateStats(e.getValue())
//                ));
//    }
//
//    private Map<String, ResolutionStats> limitToMostRecentWeeks(Map<String, List<Long>> weekly, int limit) {
//        return weekly.entrySet().stream()
//                .sorted((a, b) -> parseWeek(b.getKey()).compareTo(parseWeek(a.getKey()))) // Sort newest first
//                .limit(limit)
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        e -> calculateStats(e.getValue()),
//                        (e1, e2) -> e1,
//                        LinkedHashMap::new
//                ));
//    }
//
//    private Map<String, ResolutionStats> limitToMostRecentMonths(Map<String, List<Long>> monthly, int limit) {
//        return monthly.entrySet().stream()
//                .sorted((a, b) -> parseMonth(b.getKey()).compareTo(parseMonth(a.getKey()))) // Sort newest first
//                .limit(limit)
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        e -> calculateStats(e.getValue()),
//                        (e1, e2) -> e1,
//                        LinkedHashMap::new
//                ));
//    }


//    private LocalDate parseWeek(String weekKey) {
//        String[] parts = weekKey.split(" W");
//        String monthStr = parts[0];
//        int weekOfMonth = Integer.parseInt(parts[1]);
//
//        // Parse month name to Month enum
//        Month month = Month.from(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH).parse(monthStr));
//
//        // Use current year or pass year separately if required
//        int year = LocalDate.now().getYear();
//
//        // Get first day of the month
//        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
//
//        // Calculate start of desired week
//        WeekFields weekFields = WeekFields.ISO;
//        return firstOfMonth
//                .with(weekFields.weekOfMonth(), weekOfMonth)
//                .with(weekFields.dayOfWeek(), 1); // Monday
//    }


//    private LocalDate parseWeek(String weekKey) {
//        try {
//            String[] parts = weekKey.split(" W");
//            if (parts.length != 2) throw new IllegalArgumentException("Invalid weekKey: " + weekKey);
//            String monthStr = parts[0].trim();
//            int weekOfMonth = Integer.parseInt(parts[1].trim());
//
//            // Parse month name
//            Month month = Month.from(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH).parse(monthStr));
//
//            // Caution: Always using current year can break with historical/future data!
//            // If your key doesn't include the year, you could pass 'year' separately as a parameter.
//
//            int year = LocalDate.now().getYear(); // Or pass as argument for better correctness
//
//            LocalDate firstOfMonth = LocalDate.of(year, month, 1);
//
//            WeekFields weekFields = WeekFields.ISO;
//            return firstOfMonth
//                    .with(weekFields.weekOfMonth(), weekOfMonth)
//                    .with(weekFields.dayOfWeek(), 1); // Monday
//        } catch (Exception e) {
//            System.err.println("Bad weekKey: " + weekKey + " Exception: " + e.getMessage());
//            return LocalDate.MIN;
//        }
//    }
//
//
//    // Parses "2025-07" into LocalDate (1st of month)
////    private LocalDate parseMonth(String monthKey) {
////        return LocalDate.parse(monthKey + "-01");
////    }
//
//
//        public ResolutionTimeStatsDTO getAssigneeResolutionStats(String assigneeId) {
//            User user = userRepository.findByEmployeeId(assigneeId)
//                    .orElseThrow(() -> new UserNotFoundException("User not found with employee ID: " + assigneeId));
//
//            LocalDateTime startDateWeeks = getStartOfRecentWeeks(5);
//            LocalDateTime startDateMonths = getStartOfRecentMonths(4);
//
//            // Use the earlier start date to cover both windows
//            LocalDateTime filterStartDate = startDateWeeks.isBefore(startDateMonths) ? startDateWeeks : startDateMonths;
//
//            List<Ticket> tickets = ticketRepository.findByAssigneeAndCreatedAtBetween(user, filterStartDate, LocalDateTime.now());
//
//            List<Ticket> closedTickets = tickets.stream()
//                    .filter(t -> t.getStatus() == TicketStatus.CLOSED && t.getCreatedAt() != null && t.getUpdatedAt() != null)
//                    .collect(Collectors.toList());
//
//            Map<String, List<Long>> weekly = new HashMap<>();
//            Map<String, List<Long>> monthly = new HashMap<>();
//            List<Long> overall = new ArrayList<>();
//
//            for (Ticket t : closedTickets) {
//                long minutes = Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes();
//                overall.add(minutes);
//
//                // Group by ISO year and week number e.g. "2025-W29"
//                int weekOfYear = t.getCreatedAt().get(ChronoField.ALIGNED_WEEK_OF_YEAR);
//                int year = t.getCreatedAt().getYear();
//
//                String monthAbbrev = t.getCreatedAt().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH); // e.g. "Jul"
//            int weekOfMonth = t.getCreatedAt().get(ChronoField.ALIGNED_WEEK_OF_MONTH); // e.g. 1, 2, 3...
//            String weekKey = monthAbbrev + " W" + weekOfMonth;
////                String weekKey = year + "-W" + String.format("%02d", weekOfYear);
//                weekly.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(minutes);
//
//                // Monthly key: "2025-07"
//                String monthKey = t.getCreatedAt().getYear() + "-" + String.format("%02d", t.getCreatedAt().getMonthValue());
//                monthly.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(minutes);
//            }
//
//            Map<String, ResolutionStats> limitedWeeklyStats = limitToMostRecentWeeks(weekly, 5);
//            Map<String, ResolutionStats> limitedMonthlyStats = limitToMostRecentMonths(monthly, 4);
//
//            return new ResolutionTimeStatsDTO(
//                    calculateStats(overall),
//                    limitedWeeklyStats,
//                    limitedMonthlyStats
//            );
//        }
//
//        private LocalDateTime getStartOfRecentWeeks(int weeksBack) {
//            LocalDate now = LocalDate.now();
//            // Subtract weeksBack-1 weeks and get Monday of that week
//            LocalDate startOfWeek = now.minusWeeks(weeksBack - 1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
//            return startOfWeek.atStartOfDay();
//        }
//
//        private LocalDateTime getStartOfRecentMonths(int monthsBack) {
//            LocalDate now = LocalDate.now();
//            LocalDate startOfMonth = now.minusMonths(monthsBack - 1).withDayOfMonth(1);
//            return startOfMonth.atStartOfDay();
//        }
//
//
//    public ResolutionStats calculateStats(List<Long> times) {
//        if (times.isEmpty()) return new ResolutionStats(null, null, null);
//
//        // Convert minutes to days (double, keep fraction)
//        Double min = times.stream().mapToLong(Long::longValue).min().orElse(0) / 1440.0;
//        Double max = times.stream().mapToLong(Long::longValue).max().orElse(0) / 1440.0;
//        Double avg = times.stream().mapToLong(Long::longValue).average().orElse(0) / 1440.0;
//
//        return new ResolutionStats(avg, min, max);
//    }
//
//
//    private Map<String, ResolutionStats> limitToMostRecentWeeks(Map<String, List<Long>> weekly, int limit) {
//            return weekly.entrySet().stream()
//                    // Sort keys (e.g. "2025-W29") by year and week descending (newest first)
//                    .sorted((a, b) -> parseWeek(b.getKey()).compareTo(parseWeek(a.getKey())))
//                    .limit(limit)
//                    .collect(Collectors.toMap(
//                            Map.Entry::getKey,
//                            e -> calculateStats(e.getValue()),
//                            (e1, e2) -> e1,
//                            LinkedHashMap::new
//                    ));
//        }
//
//        private Map<String, ResolutionStats> limitToMostRecentMonths(Map<String, List<Long>> monthly, int limit) {
//            return monthly.entrySet().stream()
//                    // Sort by month descending
//                    .sorted((a, b) -> parseMonth(b.getKey()).compareTo(parseMonth(a.getKey())))
//                    .limit(limit)
//                    .collect(Collectors.toMap(
//                            Map.Entry::getKey,
//                            e -> calculateStats(e.getValue()),
//                            (e1, e2) -> e1,
//                            LinkedHashMap::new
//                    ));
//        }
//
//
//        // Parses "2025-07" into LocalDate (1st day of month)
//        private LocalDate parseMonth(String monthKey) {
//            return LocalDate.parse(monthKey + "-01");
//        }


//        public ResolutionTimeStatsDTO getAssigneeResolutionStats(String assigneeId) {
//            User user = userRepository.findByEmployeeId(assigneeId)
//                    .orElseThrow(() -> new UserNotFoundException("User not found with employee ID: " + assigneeId));
//
//            // --- Filter for tickets in current month only, up to today ---
//            LocalDate today = LocalDate.now();
//            LocalDate firstOfMonth = today.withDayOfMonth(1);
//            LocalDateTime monthStart = firstOfMonth.atStartOfDay();
//            LocalDateTime monthEnd = today.atTime(LocalTime.MAX);
//
//            List<Ticket> tickets = ticketRepository.findByAssigneeAndCreatedAtBetween(
//                    user, monthStart, monthEnd
//            );
//
//            List<Ticket> allTicketList= ticketRepository.findByAssignee(user);
//
//            // Only closed tickets with valid timestamps
//            List<Ticket> closedTickets = allTicketList.stream()
//                    .filter(t -> t.getStatus() == TicketStatus.CLOSED && t.getCreatedAt() != null && t.getUpdatedAt() != null)
//                    .collect(Collectors.toList());
//
//            // Grouping
//            Map<String, List<Long>> weekly = new LinkedHashMap<>();
//            Map<String, List<Long>> monthly = new LinkedHashMap<>();
//            List<Long> overall = new ArrayList<>();
//
//            // Get current month/year for grouping consistency
//            int thisMonth = today.getMonthValue();
//            int thisYear = today.getYear();
//
//            for (Ticket t : closedTickets) {
//                LocalDateTime created = t.getCreatedAt();
//                long minutes = Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes();
//                overall.add(minutes);
//
//                // Week key: "Jul W0", "Jul W1" etc -- for current month only
//                if (created.getMonthValue() == thisMonth && created.getYear() == thisYear) {
//                    String monthAbbrev = created.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
//                    int weekOfMonth = (created.getDayOfMonth() - 1) / 7;
//                    String weekKey = monthAbbrev + " W" + weekOfMonth;
//                    weekly.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(minutes);
//                }
//
//                // Month key: "2025-07"
//                String monthKey = created.getYear() + "-" + String.format("%02d", created.getMonthValue());
//                monthly.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(minutes);
//            }
//
//            // Only current month's weekly stats, already filtered in loop above
//            Map<String, ResolutionStats> weeklyStats = calculateGroupedStats(weekly);
//            // Only current month in monthly stats, so limit =1
//            Map<String, ResolutionStats> monthlyStats = calculateGroupedStats(monthly).entrySet().stream()
//                    .filter(e -> e.getKey().equals(thisYear + "-" + String.format("%02d", thisMonth)))
//                    .collect(Collectors.toMap(
//                            Map.Entry::getKey, Map.Entry::getValue,
//                            (a, b) -> a, LinkedHashMap::new
//                    ));
//
//            return new ResolutionTimeStatsDTO(
//                    calculateStats(overall),
//                    weeklyStats,
//                    monthlyStats
//            );
//        }
//
//        // Defensive week parser for "Jul W0" type keys
//        private LocalDate parseWeek(String weekKey) {
//            try {
//                String[] parts = weekKey.split(" W");
//                if (parts.length != 2)
//                    throw new IllegalArgumentException("Invalid weekKey: " + weekKey);
//                String monthStr = parts[0].trim();
//                int weekOfMonth = Integer.parseInt(parts[1].trim());
//
//                Month month = Month.from(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH).parse(monthStr));
//                int year = LocalDate.now().getYear(); // all data is for current month/year
//                LocalDate firstOfMonth = LocalDate.of(year, month, 1);
//                // Start of week = firstOfMonth + weekOfMonth*7
//                return firstOfMonth.plusDays(weekOfMonth * 7L);
//            } catch (Exception e) {
//                System.err.println("Bad weekKey: " + weekKey + " Exception: " + e.getMessage());
//                return LocalDate.MIN;
//            }
//        }
//
//        // Defensive month parser for "2025-07" format
//        private LocalDate parseMonth(String monthKey) {
//            try {
//                return LocalDate.parse(monthKey + "-01");
//            } catch (Exception e) {
//                System.err.println("Bad monthKey: " + monthKey + " Exception: " + e.getMessage());
//                return LocalDate.MIN;
//            }
//        }
//
//        // Groups keys already filtered for current month; so just sort for stable order
//        private Map<String, ResolutionStats> calculateGroupedStats(Map<String, List<Long>> groupedTimes) {
//            // keys are either "Jul Wn" (for weekly) or "YYYY-MM" (for monthly)
//            return groupedTimes.entrySet().stream()
//                    .sorted(Comparator.comparing(Map.Entry::getKey))
//                    .collect(Collectors.toMap(
//                            Map.Entry::getKey,
//                            e -> calculateStats(e.getValue()),
//                            (e1, e2) -> e1,
//                            LinkedHashMap::new
//                    ));
//        }
//
//        public ResolutionStats calculateStats(List<Long> times) {
//            if (times.isEmpty()) return new ResolutionStats(null, null, null);
//
//            // Convert minutes to days (as Double, fractional)
//            Double min = times.stream().mapToLong(Long::longValue).min().orElse(0) / 1440.0;
//            Double max = times.stream().mapToLong(Long::longValue).max().orElse(0) / 1440.0;
//            Double avg = times.stream().mapToLong(Long::longValue).average().orElse(0) / 1440.0;
//            return new ResolutionStats(avg, min, max);
//        }
//
//        // Reused if needed, but for current logic not called in main path
//        private LocalDateTime getStartOfRecentWeeks(int weeksBack) {
//            LocalDate now = LocalDate.now();
//            LocalDate startOfWeek = now.minusWeeks(weeksBack - 1).with(DayOfWeek.MONDAY);
//            return startOfWeek.atStartOfDay();
//        }
//
//        private LocalDateTime getStartOfRecentMonths(int monthsBack) {
//            LocalDate now = LocalDate.now();
//            LocalDate startOfMonth = now.minusMonths(monthsBack - 1).withDayOfMonth(1);
//            return startOfMonth.atStartOfDay();
//        }



        public ResolutionTimeStatsDTO getAssigneeResolutionStats(String assigneeId) {
            User user = userRepository.findByEmployeeId(assigneeId)
                    .orElseThrow(() -> new UserNotFoundException("User not found with employee ID: " + assigneeId));
            LocalDate today = LocalDate.now();
            LocalDate firstOfCurrentMonth = today.withDayOfMonth(1);
            LocalDate firstOf3rdLastMonth = firstOfCurrentMonth.minusMonths(2);

            // For monthly and weekly, use tickets from last 3 months
            List<Ticket> recentTickets = ticketRepository.findByAssigneeAndCreatedAtBetween(
                    user, firstOf3rdLastMonth.atStartOfDay(), today.atTime(LocalTime.MAX)
            );
            // For overall, use ALL tickets ever
            List<Ticket> allTickets = ticketRepository.findByAssignee(user);

            // Only closed tickets with valid createdAt and updatedAt
            List<Ticket> closedTicketsAllTime = allTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.CLOSED &&
                            t.getCreatedAt() != null && t.getUpdatedAt() != null)
                    .collect(Collectors.toList());

            List<Ticket> closedTickets3Months = recentTickets.stream()
                    .filter(t -> t.getStatus() == TicketStatus.CLOSED &&
                            t.getCreatedAt() != null && t.getUpdatedAt() != null)
                    .collect(Collectors.toList());

            // --- Overall Stats (all time) ---
            List<Long> allClosedDurations = closedTicketsAllTime.stream()
                    .map(t -> Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes())
                    .collect(Collectors.toList());

            // --- Month and Week Stats ---
            Map<String, List<Long>> monthly = new LinkedHashMap<>();
            Map<String, List<Long>> weekly = new LinkedHashMap<>();

            int thisMonth = today.getMonthValue();
            int thisYear = today.getYear();

            for (Ticket t : closedTickets3Months) {
                LocalDateTime created = t.getCreatedAt();
                long minutes = Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes();

                // --- Monthly ---
                String monthKey = created.getYear() + "-" + String.format("%02d", created.getMonthValue());
                monthly.computeIfAbsent(monthKey, k -> new ArrayList<>()).add(minutes);

                // --- Weekly ---
                if (created.getYear() == thisYear && created.getMonthValue() == thisMonth) {
                    String monthAbbrev = created.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    int weekOfMonth = ((created.getDayOfMonth() - 1) / 7) + 1;  // Weeks start at 1!
                    String weekKey = monthAbbrev + " W" + weekOfMonth;
                    weekly.computeIfAbsent(weekKey, k -> new ArrayList<>()).add(minutes);
                }
            }

            // --- Only the latest 3 months for monthly stats ---
            Map<String, ResolutionStats> monthlyStats = monthly.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                    .limit(3)
                    .sorted(Map.Entry.comparingByKey()) // Oldest first
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> calculateStats(e.getValue()),
                            (a, b) -> a, LinkedHashMap::new
                    ));

            // --- Weekly stats: pad all weeks of current month, so W1...Wn always present ---
            Map<String, ResolutionStats> weeklyStats = calculateGroupedStats(weekly);
            // Figure out which is the last week number this month (for today)
            int lastWeekIndex = ((today.getDayOfMonth() - 1) / 7) + 1;
            String monthAbbrev = today.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            for (int i = 1; i <= lastWeekIndex; i++) {
                String weekKey = monthAbbrev + " W" + i;
                weeklyStats.putIfAbsent(weekKey, new ResolutionStats(null, null, null, 0));
            }

            // Ensure week keys are sorted ascending
            weeklyStats = weeklyStats.entrySet().stream()
                    .sorted(Comparator.comparing(e -> Integer.parseInt(e.getKey().split(" W")[1])))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a,b)->a, LinkedHashMap::new
                    ));

            return new ResolutionTimeStatsDTO(
                    calculateStats(allClosedDurations),
                    weeklyStats,
                    monthlyStats
            );
        }

        // Used for grouped stats
        private Map<String, ResolutionStats> calculateGroupedStats(Map<String, List<Long>> groupedTimes) {
            return groupedTimes.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> calculateStats(e.getValue()),
                            (a, b) -> a, LinkedHashMap::new
                    ));
        }

        public ResolutionStats calculateStats(List<Long> times) {
            int ticketCount = times.size();
            if (ticketCount == 0) return new ResolutionStats(null, null, null, 0);

            Double min = times.stream().mapToLong(Long::longValue).min().orElse(0) / 1440.0;
            Double max = times.stream().mapToLong(Long::longValue).max().orElse(0) / 1440.0;
            Double avg = times.stream().mapToLong(Long::longValue).average().orElse(0) / 1440.0;
            return new ResolutionStats(avg, min, max, ticketCount);
        }























    public Map<String, Long> getTicketCountByAssignee() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = today.minusDays(29);
        LocalDateTime startOfDay = startDate.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        boolean isHrUser = user.getDepartment().name().equalsIgnoreCase("HR");

        List<Ticket> tickets = ticketRepository.findTicketsCreatedBetweenFiltered(
                startOfDay, endOfToday, isHrUser
        );

        return tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getAssignee() != null ? t.getAssignee().getUsername() : "Unassigned",
                        Collectors.counting()
                ));
    }


//    public Map<String, Long> getTopTicketReporters() {
//        ZoneId zone = ZoneId.of("Asia/Kolkata");
//        LocalDate today = LocalDate.now(zone);
//        LocalDate startDate = today.minusDays(29);
//        LocalDateTime startOfDay = startDate.atStartOfDay();
//        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
//
//        // Get all tickets from the past 30 days
//        List<Ticket> tickets = ticketRepository.findTicketsCreatedBetween(startOfDay, endOfToday);
//
//        // Fetch all users who are IT Executives from LocationAssignment
//        List<User> itExecutives = locationAssignmentRepository.findAll().stream()
//                .map(LocationAssignment::getItExecutive)
//                .distinct()
//                .toList();
//
//        Set<Long> itExecutiveIds = itExecutives.stream()
//                .map(User::getId)
//                .collect(Collectors.toSet());
//
//        // Group and count tickets by employee, excluding IT executives
//        return tickets.stream()
//                .filter(t -> t.getEmployee() != null && !itExecutiveIds.contains(t.getEmployee().getId()))
//                .collect(Collectors.groupingBy(
//                        t -> t.getEmployee().getUsername(),
//                        Collectors.counting()
//                ));
//    }

    public Map<String, Long> getTopTicketReporters() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = today.minusDays(29);
        LocalDateTime startOfDay = startDate.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

        // Get all tickets from the past 30 days
        List<Ticket> tickets = ticketRepository.findTicketsCreatedBetween(startOfDay, endOfToday);

        // Fetch all users who are IT Executives from LocationAssignment
        List<User> itExecutives = locationAssignmentRepository.findAll().stream()
                .map(LocationAssignment::getItExecutive)
                .distinct()
                .toList();

        Set<Long> itExecutiveIds = itExecutives.stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        // Group and count tickets by employee, excluding IT executives
        Map<String, Long> reporterCounts = tickets.stream()
                .filter(t -> t.getEmployee() != null && !itExecutiveIds.contains(t.getEmployee().getId()))
                .collect(Collectors.groupingBy(
                        t -> t.getEmployee().getUsername(),
                        Collectors.counting()
                ));

        // Sort by count descending and limit to top 15
        return reporterCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(15)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new // maintain order
                ));
    }


    public Map<String, Long> getTicketCountByLocation() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = today.minusDays(29);
        LocalDateTime startOfDay = startDate.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

        return ticketRepository.findTicketsCreatedBetween(startOfDay,endOfToday).stream()
                .filter(t -> t.getLocation() != null)
                .collect(Collectors.groupingBy(t -> t.getLocation().getName(), Collectors.counting()));
    }


//    public Page<TicketWithFeedbackDTO> getTicketsWithFeedback(String employeeId, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
//        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername()).orElseThrow(
//                ()->new UserNotFoundException("Authenticated user not found ")
//        );
//        TicketDepartment ticketDepartment = TicketDepartment.valueOf(user.getDepartment().name());
//
//        Page<TicketFeedback> feedbackPage = ticketFeedbackRepository.findByDepartmentAndAssignee(ticketDepartment,employeeId, pageable);
//
//        return feedbackPage.map(f -> {
//            Ticket ticket = f.getTicket();
//            return new TicketWithFeedbackDTO(
//                    ticket.getId(),
//                    ticket.getTitle(),
//                    ticket.getDescription(),
//                    ticket.getCategory(),
//                    ticket.getStatus(),
//                    ticket.getTicketDepartment(),
//                    ticket.getCreatedBy(),
//                    ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : "Unassigned",
//                    ticket.getCreatedAt(),
//                    f.getRating(),
//                    f.getMessage(),
//                    f.getSubmittedAt()
//            );
//        });
//    }


    public Page<TicketWithFeedbackDTO> getTicketsWithFeedback(String employeeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());

        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        boolean isHrUser = user.getDepartment() == Department.HR;

        Page<TicketFeedback> feedbackPage = ticketFeedbackRepository.findFilteredFeedbacks(
                isHrUser,
                employeeId != null && !employeeId.isBlank() ? employeeId : null,
                pageable
        );

        return feedbackPage.map(f -> {
            Ticket ticket = f.getTicket();
            return new TicketWithFeedbackDTO(
                    ticket.getId(),
                    ticket.getTitle(),
                    ticket.getDescription(),
                    ticket.getCategory(),
                    ticket.getStatus(),
                    ticket.getTicketDepartment(),
                    ticket.getCreatedBy(),
                    ticket.getAssignee() != null ? ticket.getAssignee().getUsername() : "Unassigned",
                    ticket.getCreatedAt(),
                    f.getRating(),
                    f.getMessage(),
                    f.getSubmittedAt()
            );
        });
    }


}
