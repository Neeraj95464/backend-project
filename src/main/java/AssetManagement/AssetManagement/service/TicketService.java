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
            System.out.println("Setting department from category: " + deptName);
            ticket.setTicketDepartment(TicketDepartment.valueOf(deptName));
        } else if (category == TicketCategory.FOCUS ) {
            String deptName = category.name();
            System.out.println("Setting department from category: " + deptName);
            ticket.setTicketDepartment(TicketDepartment.valueOf(deptName));
        }
        else {
            System.out.println("Setting department from DTO: " + ticketDTO.getTicketDepartment());
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

        String oldCategory = ticket.getCategory() != null ? ticket.getCategory().name() : null;

        ticket.setCategory(category);
        Ticket savedTicket = ticketRepository.save(ticket);

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


//    public PaginatedResponse<TicketDTO> getAllTicketsForAdmin(int page, int size, TicketStatus status) {
//        // Ensure only admin users can access this data
////        String role = SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString();
//        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
//                .orElseThrow(() -> new UserNotFoundException("Authenticated User not found "));
//        String role = user.getRole();
//        TicketDepartment userDepartment = TicketDepartment.valueOf(user.getDepartment().name());
//
//        if (!role.contains("ADMIN")) {
//            throw new RuntimeException("Access Denied: Only admins can view all tickets.");
//        }
//
//        Page<Ticket> ticketPage;
//
//        // Create PageRequest with sorting by createdAt in descending order
//        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
//
//        if (status != null) {
//
//            if ("ALL".equalsIgnoreCase(String.valueOf(status))) {
//                // Fetch all tickets without filtering by status
//                ticketPage = ticketRepository.findAll(pageRequest);
//            } else {
//                // Fetch tickets filtered by the specified status
//                ticketPage = ticketRepository.findByStatus(status, pageRequest);
//            }
//
//        } else {
//            // Fetch all tickets
//            ticketPage = ticketRepository.findAll(pageRequest);
//        }
//
//        List<TicketDTO> ticketDTOs = ticketPage.getContent().stream()
//                .map(ticketMapper::toDTO)
//                .collect(Collectors.toList());
//
//        // Return a paginated response
//        return new PaginatedResponse<>(
//                ticketDTOs,
//                ticketPage.getNumber(),
//                ticketPage.getSize(),
//                ticketPage.getTotalElements(),
//                ticketPage.getTotalPages(),
//                ticketPage.isLast()
//        );
//    }


    public PaginatedResponse<TicketDTO> getAllTicketsForAdmin(int page, int size, TicketStatus status) {
        // Fetch the authenticated user
        User user = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("Authenticated User not found"));

        String role = user.getRole();
        if (!role.equalsIgnoreCase("ADMIN")) {
            throw new RuntimeException("Access Denied: Only admins can view all tickets.");
        }

        TicketDepartment userDepartment = TicketDepartment.valueOf(user.getDepartment().name()); // Assuming Department is an enum

        // Define page request sorted by createdAt in descending order
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Ticket> ticketPage;

        // Apply status + department filtering
        if (status == null || "ALL".equalsIgnoreCase(String.valueOf(status))) {
            // Fetch tickets filtered by department only
            ticketPage = ticketRepository.findByTicketDepartment(userDepartment, pageRequest);
        } else {
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
        List<Location> locations = locationRepository.findAll();

        return locations.stream().map(location -> {
            List<LocationAssignment> assignments = locationAssignmentRepository.findByLocation(location);

            // Pick the assignment for the desired department or just the first one if only one is needed
            LocationAssignment assignment = assignments.stream()
                    .filter(a -> a.getTicketDepartment() == TicketDepartment.IT) // example, adjust as needed
                    .findFirst()
                    .orElse(null);

            TicketDepartment ticketDepartment = assignment != null ? assignment.getTicketDepartment() : TicketDepartment.IT;

            String executiveUsername = (assignment != null && assignment.getItExecutive() != null && assignment.getItExecutive().getUsername() != null)
                    ? assignment.getItExecutive().getUsername()
                    : "null";

            String managerUsername = (assignment != null && assignment.getLocationManager() != null && assignment.getLocationManager().getUsername() != null)
                    ? assignment.getLocationManager().getUsername()
                    : "null";


            return new LocationAssignmentDTO(
                    location.getId(),
                    location.getName(),
                    ticketDepartment,
                    executiveUsername,
                    managerUsername
            );
        }).toList();
    }

    @Transactional
    public ResponseEntity<LocationAssignment> assignLocation(LocationAssignmentRequest request) {
        System.out.println("Request was " + request);

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
                    System.out.println("Deleted old assignment with ID = " + existing.getId());
                });

        // Save new assignment
        LocationAssignment assignment = new LocationAssignment();
        assignment.setItExecutive(executive);
        assignment.setLocationManager(manager);
        assignment.setLocation(location);
        assignment.setTicketDepartment(request.getTicketDepartment());

        LocationAssignment savedAssignment = locationAssignmentRepository.save(assignment);
        System.out.println("Saved Assignment ID = " + savedAssignment.getId());

        return ResponseEntity.ok(savedAssignment);
    }

    // TicketService.java
    public List<AssigneeFeedbackDTO> getFeedbackGroupedByAssignee() {
        return ticketFeedbackRepository.getFeedbackGroupedByAssignee();
    }


    public ResponseEntity<String> updateFedback(Long ticketId, int rating) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        Optional<TicketFeedback> existingFeedback = ticketFeedbackRepository.findByTicket(ticket);

        if (existingFeedback.isPresent()) {
            return ResponseEntity.ok(
                    "<html><body><h2>You have already submitted feedback for this ticket. 🙏</h2></body></html>"
            );
        }

        TicketFeedback feedback = new TicketFeedback();
        feedback.setTicket(ticket);
        feedback.setRating(rating);
        feedback.setSubmittedAt(LocalDateTime.now());

        ticketFeedbackRepository.save(feedback);

        return ResponseEntity.ok(
                "<html><body><h2>Thank you for your feedback! ⭐</h2></body></html>"
        );
    }


    public String generateFeedbackEmailHtml(Long ticketId) {
        String baseUrl = "https://numerous-gem-accompanied-mac.trycloudflare.com/api/feedback"; // Your backend API endpoint

        return """
        <html>
        <body style="font-family: Arial, sans-serif;">
            <p>Hello,</p>
            <p>Your support ticket <strong>#%d</strong> has been resolved.</p>
            <p>We would love to hear your feedback!</p>
            <p>Please click a star below to rate your experience:</p>

            <p style="font-size: 24px;">
                <a href="%s?ticketId=%d&rating=1">⭐</a>
                <a href="%s?ticketId=%d&rating=2">⭐</a>
                <a href="%s?ticketId=%d&rating=3">⭐</a>
                <a href="%s?ticketId=%d&rating=4">⭐</a>
                <a href="%s?ticketId=%d&rating=5">⭐</a>
            </p>

            <p>Thank you!<br>IT Support Team</p>
        </body>
        </html>
        """.formatted(
                ticketId,
                baseUrl, ticketId,
                baseUrl, ticketId,
                baseUrl, ticketId,
                baseUrl, ticketId,
                baseUrl, ticketId
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
//        return locationAssignmentRepository.findAll()
//                .stream()
//                .map(la -> new UserIdNameDTO(
//                        la.getItExecutive().getEmployeeId(), // Use employeeId instead of ID
//                        la.getItExecutive().getUsername()
//                ))
//                .distinct()
//                .collect(Collectors.toList());
//    }

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

    public Map<String, Long> getTicketCountByAssignee() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zone);
        LocalDate startDate = today.minusDays(29);
        LocalDateTime startOfDay = startDate.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

        List<Ticket> tickets = ticketRepository.findTicketsCreatedBetween(startOfDay,endOfToday);
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
                        (t.getStatus() == TicketStatus.CLOSED))
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

    public ResolutionTimeStatsDTO getAssigneeResolutionStats(String assigneeId) {
        User user = userRepository.findByEmployeeId(assigneeId)
                .orElseThrow(() -> new UserNotFoundException("User not found with employee ID: " + assigneeId));

        List<Ticket> tickets = ticketRepository.findByAssignee(user);

        List<Long> resolutionTimesInMinutes = tickets.stream()
                .filter(t -> t.getCreatedAt() != null && t.getUpdatedAt() != null && t.getStatus() == TicketStatus.CLOSED)
                .map(t -> Duration.between(t.getCreatedAt(), t.getUpdatedAt()).toMinutes())
                .collect(Collectors.toList());

        if (resolutionTimesInMinutes.isEmpty()) {
            return new ResolutionTimeStatsDTO(null, null, null); // or use -1 for all if nulls are not acceptable
        }

        long avg = (long) resolutionTimesInMinutes.stream().mapToLong(Long::longValue).average().orElse(0);
        long min = resolutionTimesInMinutes.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = resolutionTimesInMinutes.stream().mapToLong(Long::longValue).max().orElse(0);

        return new ResolutionTimeStatsDTO(avg, min, max);
    }



//    public List<TicketStatusTimeSeriesDTO> getTicketStatusOverTime() {
//
//        List<Ticket> tickets = ticketRepository.findAll();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        Map<String, Map<TicketStatus, Long>> dailyStatusMap = new TreeMap<>();
//
//        for (Ticket ticket : tickets) {
//            if (ticket.getCreatedAt() != null && ticket.getStatus() != null) {
//                String date = ticket.getCreatedAt().format(formatter);
//                dailyStatusMap.putIfAbsent(date, new EnumMap<>(TicketStatus.class));
//                Map<TicketStatus, Long> statusMap = dailyStatusMap.get(date);
//                statusMap.put(ticket.getStatus(), statusMap.getOrDefault(ticket.getStatus(), 0L) + 1);
//            }
//        }
//
//        List<TicketStatusTimeSeriesDTO> series = new ArrayList<>();
//        for (Map.Entry<String, Map<TicketStatus, Long>> entry : dailyStatusMap.entrySet()) {
//            series.add(new TicketStatusTimeSeriesDTO(entry.getKey(), entry.getValue()));
//        }
//
//        return series;
//    }

//    public Map<String, Long> getTopTicketReporters() {
//        ZoneId zone = ZoneId.of("Asia/Kolkata");
//        LocalDate today = LocalDate.now(zone);
//        LocalDate startDate = today.minusDays(29);
//        LocalDateTime startOfDay = startDate.atStartOfDay();
//        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
//
//        List<Ticket> tickets = ticketRepository.findTicketsCreatedBetween(startOfDay,endOfToday);
//        return tickets.stream()
//                .collect(Collectors.groupingBy(
//                        t -> t.getEmployee() != null ? t.getEmployee().getUsername() : "Unknown",
//                        Collectors.counting()
//                ));
//    }

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

//    public List<Ticket> getOverdueTickets() {
//        ZoneId zone = ZoneId.of("Asia/Kolkata");
//        LocalDate today = LocalDate.now(zone);
//        LocalDate startDate = today.minusDays(29); // last 30 days
//        LocalDateTime startOfDay = startDate.atStartOfDay();
//        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
//        LocalDateTime now = LocalDateTime.now(zone);
//
//        List<Ticket> tickets = ticketRepository.findTicketsCreatedBetween(startOfDay, endOfToday);
//
//        List<Ticket> filtered = tickets.stream()
//                .filter(t -> t.getDueDate() != null &&
//                        t.getDueDate().isBefore(now) &&
//                        (t.getStatus() == TicketStatus.OPEN || t.getStatus() == TicketStatus.WAITING))
//                .toList();
//
//        System.out.println("Total found: " + filtered.size());
//        filtered.forEach(t -> System.out.println("Ticket: " + t.getId() + ", Due: " + t.getDueDate() + ", Status: " + t.getStatus()));
//
//        return filtered;
//    }



}
