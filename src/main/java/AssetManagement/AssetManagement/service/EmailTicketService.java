package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.TicketCategory;
import AssetManagement.AssetManagement.dto.TicketDTO;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketMessage;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.TicketStatus;
import AssetManagement.AssetManagement.exception.UserNotFoundException;
import AssetManagement.AssetManagement.mapper.TicketMapper;
import AssetManagement.AssetManagement.repository.TicketMessageRepository;
import AssetManagement.AssetManagement.repository.TicketRepository;
import AssetManagement.AssetManagement.repository.UserRepository;
import AssetManagement.AssetManagement.util.AuthUtils;
import jakarta.annotation.Nullable;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailTicketService {

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TicketMessageRepository ticketMessageRepository;

    @Autowired
    private JavaMailSender mailSender; // Inject Spring Mail Sender


    @Scheduled(fixedRate = 60000) // Runs every 1 minute
    public void checkEmailsForTickets() {
        try {
            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imaps.host", "imap.gmail.com");
            properties.put("mail.imaps.port", "993");

            Session emailSession = Session.getDefaultInstance(properties);
            Store store = emailSession.getStore("imaps");
            store.connect("imap.gmail.com", "jubileehills.mahavirauto@gmail.com", "rhav ciqd ivhp gemz");

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.getMessages();
            for (Message message : messages) {
                if (!message.isSet(Flags.Flag.SEEN)) { // Only process unread emails
                    MimeMessage mimeMessage = (MimeMessage) message;
                    String subject = mimeMessage.getSubject();
                    String senderEmail = ((InternetAddress) mimeMessage.getFrom()[0]).getAddress();

                    List<String> ccEmails = extractRecipients(mimeMessage);

//                    System.out.println("Received Email - Subject: " + subject);
//                    System.out.println("From: " + senderEmail);
                    System.out.println("CC Recipients: " + ccEmails);

                    String content = extractTextFromMessage(mimeMessage); // Extract message text
                    System.out.println("Received Email - Subject: " + subject);
                    System.out.println("Content: " + content);
                    System.out.println("From: " + senderEmail);

                    // ✅ Check if email is a reply to an existing ticket
                    Long ticketId = extractTicketIdFromSubject(subject);
                    if (ticketId != null) {
                        saveReplyToTicket(ticketId, senderEmail, content);
                    } else {
                        // ✅ If not a reply, create a new ticket
                        TicketDTO ticketDTO = createTicketFromEmail(subject, content, senderEmail);
//                        sendAcknowledgmentEmail(mimeMessage, ticketDTO, ccEmails); // Send reply as acknowledgment

                        sendTicketAcknowledgmentEmail(
                                senderEmail,
                                ticketRepository.findById(ticketDTO.getId()).orElseThrow(),
                                ccEmails,
                                mimeMessage.getHeader("Message-ID", null),
                                mimeMessage.getSubject()
                        );

                    }

                    message.setFlag(Flags.Flag.SEEN, true); // Mark as read
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @Transactional
//    public TicketDTO createTicketFromEmail(String emailSubject, String emailBody, String senderEmail) {
//        Ticket ticket = new Ticket();
//        ticket.setTitle(emailSubject); // Email subject as ticket title
//        ticket.setDescription(emailBody); // Email body as ticket description
//        ticket.setCategory(TicketCategory.OTHER); // Default category (update as needed)
//        ticket.setStatus(TicketStatus.OPEN);
//        System.out.println("your email is "+senderEmail);
//        ticket.setEmployee(userRepository.findByEmail(senderEmail)
//                .orElseThrow(() -> new UserNotFoundException("User not found")));
//        System.out.println("your user is "+userRepository.findByEmail(senderEmail)
//                .orElseThrow(() -> new UserNotFoundException("User not found")));
//        ticket.setCreatedAt(LocalDateTime.now());
//        ticket.setUpdatedAt(LocalDateTime.now());
//
//        System.out.println("your ticket is "+ticket);
//        Ticket savedTicket = ticketRepository.save(ticket);
//        return ticketMapper.toDTO(savedTicket);
//    }

    @Transactional
    public TicketDTO createTicketFromEmail(String emailSubject, String emailBody, String senderEmail) {
        Ticket ticket = new Ticket();
        ticket.setTitle(emailSubject);
        ticket.setDescription(emailBody);
        ticket.setCategory(TicketCategory.OTHER);
//        ticket.setStatus(TicketStatus.OPEN);

        System.out.println("Sender email: " + senderEmail);

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        System.out.println("User found - Email: " + sender.getEmail());

        ticket.setEmployee(sender);

        if (ticket.getAssignee() == null) {
            ticket.setStatus(TicketStatus.UNASSIGNED);
        } else {
            ticket.setStatus(TicketStatus.OPEN);
        }

        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket savedTicket = ticketRepository.save(ticket);
        return ticketMapper.toDTO(savedTicket);
    }


    private List<String> extractRecipients(Message message) throws MessagingException {
        List<String> recipients = new ArrayList<>();

        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);

        // ✅ Add 'To' recipients
        if (toAddresses != null) {
            for (Address address : toAddresses) {
                String email = ((InternetAddress) address).getAddress();
                if (!email.equalsIgnoreCase("jubileehills.mahavirauto@gmail.com")) { // Exclude main email
                    recipients.add(email);
                }
            }
        }

        // ✅ Add 'CC' recipients
        if (ccAddresses != null) {
            for (Address address : ccAddresses) {
                String email = ((InternetAddress) address).getAddress();
                if (!email.equalsIgnoreCase("jubileehills.mahavirauto@gmail.com")) { // Exclude main email
                    recipients.add(email);
                }
            }
        }

        return recipients;
    }


    private Long extractTicketIdFromSubject(String subject) {
        Pattern pattern = Pattern.compile("Ticket ID: (\\d+)");
        Matcher matcher = pattern.matcher(subject);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1)); // Extract ticket ID
        }
        return null; // No ticket ID found (new ticket)
    }


    // ✅ Extracts text from emails, even if they contain HTML or attachments
    private String extractTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("text/html")) {
            return org.jsoup.Jsoup.parse(message.getContent().toString()).text(); // Remove HTML tags
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return extractTextFromMimeMultipart(mimeMultipart);
        }
        return "";
    }

    private String extractTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                return bodyPart.getContent().toString();
            } else if (bodyPart.isMimeType("text/html")) {
                return org.jsoup.Jsoup.parse(bodyPart.getContent().toString()).text();
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(extractTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }

    private String extractLatestReply(String fullMessage) {
        String[] lines = fullMessage.split("\n");
        StringBuilder latestReply = new StringBuilder();

        for (String line : lines) {
            // Stop reading if the line contains previous email indicators
            if (line.startsWith(">") || line.contains("On ") || line.contains("wrote:") || line.contains("From:")) {
                break;
            }
            latestReply.append(line).append("\n");
        }

        return latestReply.toString().trim();
    }

    public void sendReplyToTicket(Long ticketId, String messageContent) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));

        TicketMessage firstMessage = ticketMessageRepository.findTopByTicketOrderBySentAtAsc(ticket);
        String recipient = ticket.getEmployee().getEmail();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipient);
//            helper.setSubject("Ticket ID: " + ticket.getId() + " - " + ticket.getTitle());
            helper.setSubject(ticket.getTitle());
            helper.setText("New update on your ticket:\n\n" + messageContent, false);

            // ✅ Maintain email thread with proper headers
            if (firstMessage != null && firstMessage.getMessageId() != null) {
                message.setHeader("In-Reply-To", firstMessage.getMessageId());
                message.setHeader("References", firstMessage.getMessageId());
            }

            message.saveChanges(); // Ensures Message-ID is generated

            // ✅ Send message
            mailSender.send(message);

            // ✅ Save this reply with the new Message-ID for future threading
            String messageId = message.getMessageID(); // Extract after saveChanges()

            TicketMessage replyMessage = new TicketMessage();
            replyMessage.setTicket(ticket);
            replyMessage.setSender(ticket.getEmployee()); // Or logged-in user
            replyMessage.setMessage(messageContent);
            replyMessage.setSentAt(LocalDateTime.now());
            replyMessage.setMessageId(messageId); // ✅ Save new message ID

            ticketMessageRepository.save(replyMessage);

            System.out.println("✅ Reply sent for Ticket ID: " + ticketId + " with Message-ID: " + messageId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void saveReplyToTicket(Long ticketId, String senderEmail, String messageContent) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // ✅ Extract only the latest reply
        String latestReply = extractLatestReply(messageContent);

        // ✅ Ensure message is within a safe length limit
        if (latestReply.length() > 5000) {
            latestReply = latestReply.substring(0, 5000) + "...";
        }

        TicketMessage ticketMessage = new TicketMessage();
        ticketMessage.setTicket(ticket);
        ticketMessage.setSender(sender);
        ticketMessage.setMessage(latestReply);
        ticketMessage.setSentAt(LocalDateTime.now());

        ticketMessageRepository.save(ticketMessage);
        System.out.println("✅ Saved latest reply for Ticket ID: " + ticketId);
    }


    public void sendAcknowledgmentReplyToTicket(Long ticketId, String messageContent, String inReplyToMessageId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));

        // Get the currently logged-in user
        User sender = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String recipientEmail;
            String ccEmail = null;

            // Determine who should be in "To" and who should be in "Cc"
            if (sender.getId().equals(ticket.getEmployee().getId())) {
                // Sender is the employee
                if (ticket.getAssignee() != null) {
                    recipientEmail = ticket.getAssignee().getEmail(); // Send to assignee
                    ccEmail = sender.getEmail(); // CC the employee
                } else {
                    recipientEmail = "manager@example.com"; // Default recipient
                }
            } else if (ticket.getAssignee() != null && sender.getId().equals(ticket.getAssignee().getId())) {
                // Sender is the assignee
                recipientEmail = ticket.getEmployee().getEmail(); // Send to employee
                ccEmail = sender.getEmail(); // CC the assignee
            } else {
                // Sender is neither the employee nor assignee — fallback to manager
                recipientEmail = "kumarneerajkumar1781@gmail.com";
            }

            helper.setTo(recipientEmail);

            if (ccEmail != null) {
                helper.setCc(ccEmail);
            }

            helper.setSubject("Re: Ticket ID: " + ticket.getId() + " - " + ticket.getTitle());
            helper.setText("Your message has been received:\n\n" + messageContent, false);

            // Email threading headers
            if (inReplyToMessageId != null) {
                message.setHeader("In-Reply-To", inReplyToMessageId);
                message.setHeader("References", inReplyToMessageId);
            }

            message.saveChanges(); // Required for Message-ID
            String messageId = message.getMessageID();

            mailSender.send(message);

            // Save message to DB
//            TicketMessage replyMessage = new TicketMessage();
//            replyMessage.setTicket(ticket);
//            replyMessage.setSender(sender);
//            replyMessage.setMessage(messageContent);
//            replyMessage.setSentAt(LocalDateTime.now());
//            replyMessage.setMessageId(messageId);

//            ticketMessageRepository.save(replyMessage);

            System.out.printf("✅ Acknowledgment reply sent to %s%s%n",
                    recipientEmail, ccEmail != null ? " (cc: " + ccEmail + ")" : "");

        } catch (Exception e) {
            System.err.println("❌ Failed to send acknowledgment reply for ticket ID: " + ticketId);
            e.printStackTrace();
        }
    }

    public void sendTicketAcknowledgmentEmail(
            String recipientEmail,
            Ticket ticket,
            @Nullable List<String> ccEmails,
            @Nullable String inReplyToMessageId,
            @Nullable String originalSubject
    ) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // Subject - add ticket ID and fallback to title
            String subject = "Ticket ID: " + ticket.getId() + " - " +
                    (originalSubject != null ? originalSubject : ticket.getTitle());

            helper.setTo(recipientEmail);
            helper.setSubject(subject);

            // Optional CCs
            if (ccEmails != null && !ccEmails.isEmpty()) {
                helper.setCc(ccEmails.toArray(new String[0]));
            }

            // HTML email content
            String htmlContent = """
                <html>
                    <body style="font-family: Arial, sans-serif;">
                        <p>Dear User,</p>
                        <p>Your support ticket has been created successfully with the following details:</p>
                        <table style="border-collapse: collapse; width: 100%%;">
                            <tr><td><strong>Ticket ID:</strong></td><td>%d</td></tr>
                            <tr><td><strong>Title:</strong></td><td>%s</td></tr>
                            <tr><td><strong>Status:</strong></td><td>%s</td></tr>
                            <tr><td><strong>Created At:</strong></td><td>%s</td></tr>
                        </table>
                        <p>We will get back to you shortly.</p>
                        <br>
                        <p>Best Regards,<br><strong>IT Support Team</strong></p>
                    </body>
                </html>
                """.formatted(
                    ticket.getId(),
                    ticket.getTitle(),
                    ticket.getStatus().name(),
                    ticket.getCreatedAt().toString()
            );

            helper.setText(htmlContent, true); // HTML enabled

            // Threading headers for reply
            if (inReplyToMessageId != null) {
                message.setHeader("In-Reply-To", inReplyToMessageId);
                message.setHeader("References", inReplyToMessageId);
            }

// Generate and store new Message-ID
            message.saveChanges(); // Required for getMessageID()
            String newMessageId = message.getMessageID();
            ticket.setMessageId(newMessageId); // Save for future threading
            ticketRepository.save(ticket);

            mailSender.send(message);
            System.out.println("✅ Acknowledgment email sent to " + recipientEmail);
            System.out.println("your message id "+ticket.getMessageId());

        } catch (Exception e) {
            System.err.println("❌ Failed to send acknowledgment email to " + recipientEmail);
            e.printStackTrace();
        }
    }

}
