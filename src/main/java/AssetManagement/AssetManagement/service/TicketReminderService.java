package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import AssetManagement.AssetManagement.repository.LocationAssignmentRepository;
import AssetManagement.AssetManagement.repository.TicketRepository;
import AssetManagement.AssetManagement.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketReminderService {

    private final TicketRepository ticketRepository;
    private final EmailService EmailService; // Your mail service
    private final UserRepository userRepository;
    private final LocationAssignmentRepository locationAssignmentRepository;
    private final EmailService emailService;

    public TicketReminderService(TicketRepository ticketRepository,
                                 EmailService EmailService,
                                 UserRepository userRepository, LocationAssignmentRepository locationAssignmentRepository, AssetManagement.AssetManagement.service.EmailService emailService) {
        this.ticketRepository = ticketRepository;
        this.EmailService = EmailService;
        this.userRepository = userRepository;
        this.locationAssignmentRepository = locationAssignmentRepository;
        this.emailService = emailService;
    }
    @Scheduled(cron = "0 0 9 * * *") // Runs daily at 9 AM
    public void sendReminderForOldTickets() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Ticket> oldOpenTickets = ticketRepository.findOpenTicketsOlderThan(sevenDaysAgo);

        // Map<Manager, List<Ticket>>
        Map<User, List<Ticket>> ticketsByManager = new HashMap<>();

        for (Ticket ticket : oldOpenTickets) {
            TicketDepartment department = ticket.getTicketDepartment();
            Location location = ticket.getLocation();

            // Fetch manager based on location and department
            User manager = locationAssignmentRepository
                    .findLocationManagerByLocationAndTicketDepartment(location, department);

            if (manager != null) {
                ticketsByManager.computeIfAbsent(manager, k -> new ArrayList<>()).add(ticket);
            }
        }

        // Send grouped emails to each manager
        for (Map.Entry<User, List<Ticket>> entry : ticketsByManager.entrySet()) {
            User manager = entry.getKey();
            List<Ticket> tickets = entry.getValue();

            String emailBody = buildEmailBody(manager, tickets);
            String subject = "Reminder: Open Tickets Over 7 Days for Your Department";

            emailService.sendEmailViaGraph(manager.getEmail(), subject, emailBody, null);
        }
    }


    private String buildEmailBody(User manager, List<Ticket> tickets) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h3>Dear ").append(manager.getUsername()).append(",</h3>");
        html.append("<p>The following tickets are open for more than 7 days:</p>");
        html.append("<table border='1' cellpadding='5' cellspacing='0'>")
                .append("<tr><th>Ticket ID</th><th>Title</th><th>Created At</th><th>Department</th><th>Location</th><th>Employee</th></tr>");

        for (Ticket t : tickets) {
            html.append("<tr>")
                    .append("<td>").append(t.getId()).append("</td>")
                    .append("<td>").append(t.getTitle()).append("</td>")
                    .append("<td>").append(t.getCreatedAt()).append("</td>")
                    .append("<td>").append(t.getTicketDepartment()).append("</td>")
                    .append("<td>").append(t.getLocation().getName()).append("</td>")
                    .append("<td>").append(t.getEmployee().getUsername()).append("</td>")
                    .append("</tr>");
        }

        html.append("</table>");
        html.append("<p>Please take the necessary action at the earliest.</p>");
        html.append("<br><p>Regards,<br>IT Helpdesk System</p>");
        html.append("</body></html>");
        return html.toString();
    }

}
