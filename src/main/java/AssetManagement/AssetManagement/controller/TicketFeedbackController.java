package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class TicketFeedbackController {

    private final TicketService ticketService;

    public TicketFeedbackController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<String> submitFeedback(
            @RequestParam Long ticketId,
            @RequestParam int rating
    ) {
        System.out.println("Request came with ticketId = " + ticketId + ", rating = " + rating);
        return ticketService.updateFedback(ticketId, rating);
    }


    @GetMapping("/test")
    public ResponseEntity<String> test(

    ) {
        return ResponseEntity.ok("<html><body><h2>Thank you for your! ⭐</h2></body></html>");
    }

}
