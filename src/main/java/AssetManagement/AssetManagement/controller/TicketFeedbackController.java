package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketFeedback;
import AssetManagement.AssetManagement.repository.TicketFeedbackRepository;
import AssetManagement.AssetManagement.repository.TicketRepository;
import AssetManagement.AssetManagement.service.TicketService;
import com.azure.core.annotation.Post;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/feedback")
public class TicketFeedbackController {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final TicketFeedbackRepository ticketFeedbackRepository;

    public TicketFeedbackController(TicketService ticketService, TicketRepository ticketRepository, TicketFeedbackRepository ticketFeedbackRepository) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.ticketFeedbackRepository = ticketFeedbackRepository;
    }


    @GetMapping
    public ResponseEntity<String> submitRating(@RequestParam Long ticketId, @RequestParam int rating) {
        System.out.println("🔵 Received rating request: ticketId=" + ticketId + ", rating=" + rating);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        Optional<TicketFeedback> existing = ticketFeedbackRepository.findByTicket(ticket);
        if (existing.isPresent()) {
            System.out.println("⚠️ Rating already exists for ticket: " + ticketId);
            return ResponseEntity.ok("<html><body><h2>You have already submitted feedback 🙏</h2></body></html>");
        }

        TicketFeedback feedback = new TicketFeedback();
        feedback.setTicket(ticket);
        feedback.setRating(rating);
        feedback.setSubmittedAt(LocalDateTime.now());
        ticketFeedbackRepository.save(feedback);

        System.out.println("✅ Rating saved. Checking if detailed feedback is needed...");

        if (rating == 1 || rating == 5) {
            String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Feedback</title></head>
            <body>
                <h2>Tell us more about your experience</h2>
                <form method="get" action="https://mahavir-asset.duckdns.org:7355/api/feedback/message">
                    <input type="hidden" name="ticketId" value="%d" />
                    <textarea name="message" rows="5" cols="50" placeholder="Write your feedback here..." required></textarea><br/>
                    <button type="submit">Submit</button>
                </form>
            </body>
            </html>
        """.formatted(ticketId);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        }

        return ResponseEntity.ok("<html><body><h2>Thank you for your feedback! ⭐</h2></body></html>");
    }


        @GetMapping("/message")
        public ResponseEntity<String> submitMessage(@RequestParam Long ticketId, @RequestParam String message) {
            System.out.println("🟢 Received message submission for ticketId=" + ticketId + " | message=" + message);

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("<html><body><h3>Feedback message cannot be empty.</h3></body></html>");
            }

            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("Ticket not found"));

            TicketFeedback feedback = ticketFeedbackRepository.findByTicket(ticket)
                    .orElseThrow(() -> new RuntimeException("No feedback record found for this ticket."));

            if (feedback.getMessage() != null && !feedback.getMessage().isBlank()) {
                return ResponseEntity.ok("<html><body><h3>You have already submitted your message. 🙏</h3></body></html>");
            }

            feedback.setMessage(message.trim());
            ticketFeedbackRepository.save(feedback);

            return ResponseEntity.ok("<html><body><h2>Thanks for your detailed feedback! 🙏</h2></body></html>");
        }

}
