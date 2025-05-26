//package AssetManagement.AssetManagement.service;
//
//import AssetManagement.AssetManagement.enums.TicketCategory;
//import AssetManagement.AssetManagement.dto.TicketDTO;
//import AssetManagement.AssetManagement.entity.Ticket;
//import AssetManagement.AssetManagement.entity.TicketMessage;
//import AssetManagement.AssetManagement.entity.User;
//import AssetManagement.AssetManagement.enums.TicketStatus;
//import AssetManagement.AssetManagement.exception.UserNotFoundException;
//import AssetManagement.AssetManagement.mapper.TicketMapper;
//import AssetManagement.AssetManagement.repository.TicketMessageRepository;
//import AssetManagement.AssetManagement.repository.TicketRepository;
//import AssetManagement.AssetManagement.repository.UserRepository;
//import AssetManagement.AssetManagement.util.AuthUtils;
//import jakarta.annotation.Nullable;
//import jakarta.mail.*;
//import jakarta.mail.internet.InternetAddress;
//import jakarta.mail.internet.MimeMessage;
//import jakarta.mail.internet.MimeMultipart;
//import jakarta.mail.search.FlagTerm;
//import jakarta.persistence.EntityNotFoundException;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.mail.javamail.MimeMessageHelper;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//@Service
//public class EmailTicketService {
//
//    @Autowired
//    private TicketRepository ticketRepository;
//    @Autowired
//    private TicketMapper ticketMapper;
//    @Autowired
//    private UserRepository userRepository;
//    @Autowired
//    private TicketMessageRepository ticketMessageRepository;
//
//    @Autowired
//    private JavaMailSender mailSender; // Inject Spring Mail Sender
////
////
////    @Scheduled(fixedRate = 60000) // Runs every 1 minute
////    public void checkEmailsForTickets() {
////        Store store = null;
////        Folder inbox = null;
////
////        try {
////            Properties properties = new Properties();
////            properties.put("mail.store.protocol", "imaps");
////            properties.put("mail.imaps.host", "imap.gmail.com");
////            properties.put("mail.imaps.port", "993");
////
////            Session emailSession = Session.getDefaultInstance(properties);
////            store = emailSession.getStore("imaps");
////
////            // Avoid hardcoding credentials; load from secure config or environment
////            String username = "jubileehills.mahavirauto@gmail.com";
////            String password = "rhav ciqd ivhp gemz";
////            store.connect("imap.gmail.com", username, password);
////
////            inbox = store.getFolder("INBOX");
////            inbox.open(Folder.READ_WRITE);
////
////            // Fetch only last 20 messages to avoid memory or server issues
////            int messageCount = inbox.getMessageCount();
////            int start = Math.max(1, messageCount - 19);
////            Message[] messages = inbox.getMessages(start, messageCount);
////
////            for (Message message : messages) {
////                try {
////                    Flags flags = message.getFlags();
////                    if (!flags.contains(Flags.Flag.SEEN)) {
////                        MimeMessage mimeMessage = (MimeMessage) message;
////                        String subject = mimeMessage.getSubject();
////                        String senderEmail = ((InternetAddress) mimeMessage.getFrom()[0]).getAddress();
////
////                        List<String> ccEmails = extractRecipients(mimeMessage);
////                        String content = extractTextFromMessage(mimeMessage);
////
////                        System.out.println("Received Email - Subject: " + subject);
////                        System.out.println("Content: " + content);
////                        System.out.println("From: " + senderEmail);
////                        System.out.println("CC Recipients: " + ccEmails);
////
////                        Long ticketId = extractTicketIdFromSubject(subject);
////                        if (ticketId != null) {
////                            saveReplyToTicket(ticketId, senderEmail, content,ccEmails);
////                        } else {
////                            TicketDTO ticketDTO = createTicketFromEmail(subject, content, senderEmail);
////                            sendTicketAcknowledgmentEmail(
////                                    senderEmail,
////                                    ticketRepository.findById(ticketDTO.getId()).orElseThrow(),
////                                    ccEmails,
////                                    mimeMessage.getHeader("Message-ID", null),
////                                    mimeMessage.getSubject()
////                            );
////                        }
////
////                        message.setFlag(Flags.Flag.SEEN, true); // Mark as read
////                    }
////                } catch (Exception emailEx) {
////                    // Log but continue processing remaining emails
////                    System.err.println("Failed to process an email: " + emailEx.getMessage());
////                    emailEx.printStackTrace();
////                }
////            }
////
////        } catch (Exception e) {
////            System.err.println("Error during email polling: " + e.getMessage());
////            e.printStackTrace();
////        } finally {
////            try {
////                if (inbox != null && inbox.isOpen()) {
////                    inbox.close(false);
////                }
////                if (store != null) {
////                    store.close();
////                }
////            } catch (MessagingException me) {
////                System.err.println("Error closing mail resources: " + me.getMessage());
////                me.printStackTrace();
////            }
////        }
////    }
//
//
//
//
//    @Transactional
//    public TicketDTO createTicketFromEmail(String emailSubject, String emailBody, String senderEmail) {
//        Ticket ticket = new Ticket();
//        ticket.setTitle(emailSubject);
//        ticket.setDescription(emailBody);
//        ticket.setCategory(TicketCategory.OTHER);
////        ticket.setStatus(TicketStatus.OPEN);
//
//        System.out.println("Sender email: " + senderEmail);
//
//        User sender = userRepository.findByEmail(senderEmail)
//                .orElseThrow(() -> new UserNotFoundException("User not found"));
//
//        System.out.println("User found - Email: " + sender.getEmail());
//
//        ticket.setEmployee(sender);
//
//        if (ticket.getAssignee() == null) {
//            ticket.setStatus(TicketStatus.UNASSIGNED);
//        } else {
//            ticket.setStatus(TicketStatus.OPEN);
//        }
//
//        ticket.setCreatedAt(LocalDateTime.now());
//        ticket.setUpdatedAt(LocalDateTime.now());
//
//        Ticket savedTicket = ticketRepository.save(ticket);
//        return ticketMapper.toDTO(savedTicket);
//    }
//
//
//    private List<String> extractRecipients(Message message) throws MessagingException {
//        List<String> recipients = new ArrayList<>();
//
//        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
//        Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);
//
//        // ✅ Add 'To' recipients
//        if (toAddresses != null) {
//            for (Address address : toAddresses) {
//                String email = ((InternetAddress) address).getAddress();
//                if (!email.equalsIgnoreCase("jubileehills.mahavirauto@gmail.com")) { // Exclude main email
//                    recipients.add(email);
//                }
//            }
//        }
//
//        // ✅ Add 'CC' recipients
//        if (ccAddresses != null) {
//            for (Address address : ccAddresses) {
//                String email = ((InternetAddress) address).getAddress();
//                if (!email.equalsIgnoreCase("jubileehills.mahavirauto@gmail.com")) { // Exclude main email
//                    recipients.add(email);
//                }
//            }
//        }
//
//        return recipients;
//    }
//
//
//    private Long extractTicketIdFromSubject(String subject) {
//        Pattern pattern = Pattern.compile("Ticket ID: (\\d+)");
//        Matcher matcher = pattern.matcher(subject);
//        if (matcher.find()) {
//            return Long.parseLong(matcher.group(1)); // Extract ticket ID
//        }
//        return null; // No ticket ID found (new ticket)
//    }
//
//
//    // ✅ Extracts text from emails, even if they contain HTML or attachments
//    private String extractTextFromMessage(Message message) throws Exception {
//        if (message.isMimeType("text/plain")) {
//            return message.getContent().toString();
//        } else if (message.isMimeType("text/html")) {
//            return org.jsoup.Jsoup.parse(message.getContent().toString()).text(); // Remove HTML tags
//        } else if (message.isMimeType("multipart/*")) {
//            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
//            return extractTextFromMimeMultipart(mimeMultipart);
//        }
//        return "";
//    }
//
//    private String extractTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
//        StringBuilder result = new StringBuilder();
//        for (int i = 0; i < mimeMultipart.getCount(); i++) {
//            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
//            if (bodyPart.isMimeType("text/plain")) {
//                return bodyPart.getContent().toString();
//            } else if (bodyPart.isMimeType("text/html")) {
//                return org.jsoup.Jsoup.parse(bodyPart.getContent().toString()).text();
//            } else if (bodyPart.getContent() instanceof MimeMultipart) {
//                result.append(extractTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
//            }
//        }
//        return result.toString();
//    }
//
//    private String extractLatestReply(String fullMessage) {
//        String[] lines = fullMessage.split("\n");
//        StringBuilder latestReply = new StringBuilder();
//
//        for (String line : lines) {
//            // Stop reading if the line contains previous email indicators
//            if (line.startsWith(">") || line.contains("On ") || line.contains("wrote:") || line.contains("From:")) {
//                break;
//            }
//            latestReply.append(line).append("\n");
//        }
//
//        return latestReply.toString().trim();
//    }
//
//    public void sendReplyToTicket(Long ticketId, String messageContent) {
//        Ticket ticket = ticketRepository.findById(ticketId)
//                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
//
//        TicketMessage firstMessage = ticketMessageRepository.findTopByTicketOrderBySentAtAsc(ticket);
//        String recipient = ticket.getEmployee().getEmail();
//
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//
//            helper.setTo(recipient);
////            helper.setSubject("Ticket ID: " + ticket.getId() + " - " + ticket.getTitle());
//            helper.setSubject(ticket.getTitle());
//            helper.setText("New update on your ticket:\n\n" + messageContent, false);
//
//            // ✅ Maintain email thread with proper headers
//            if (firstMessage != null && firstMessage.getMessageId() != null) {
//                message.setHeader("In-Reply-To", firstMessage.getMessageId());
//                message.setHeader("References", firstMessage.getMessageId());
//            }
//
//            message.saveChanges(); // Ensures Message-ID is generated
//
//            // ✅ Send message
//            mailSender.send(message);
//
//            // ✅ Save this reply with the new Message-ID for future threading
//            String messageId = message.getMessageID(); // Extract after saveChanges()
//
//            TicketMessage replyMessage = new TicketMessage();
//            replyMessage.setTicket(ticket);
//            replyMessage.setSender(ticket.getEmployee()); // Or logged-in user
//            replyMessage.setMessage(messageContent);
//            replyMessage.setSentAt(LocalDateTime.now());
//            replyMessage.setMessageId(messageId); // ✅ Save new message ID
//
//            ticketMessageRepository.save(replyMessage);
//
//            System.out.println("✅ Reply sent for Ticket ID: " + ticketId + " with Message-ID: " + messageId);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//// this method is called when we are sending mail in already created ticket to add like message.
//    private void saveReplyToTicket(Long ticketId, String senderEmail, String messageContent,List<String> ccEmails) {
//        Ticket ticket = ticketRepository.findById(ticketId)
//                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
//
//        User sender = userRepository.findByEmail(senderEmail)
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));
//
//        // ✅ Extract only the latest reply
//        String latestReply = extractLatestReply(messageContent);
//
//        // ✅ Ensure message is within a safe length limit
//        if (latestReply.length() > 5000) {
//            latestReply = latestReply.substring(0, 5000) + "...";
//        }
//
//        TicketMessage ticketMessage = new TicketMessage();
//        ticketMessage.setTicket(ticket);
//        ticketMessage.setSender(sender);
//        ticketMessage.setMessage(latestReply);
//        ticketMessage.setSentAt(LocalDateTime.now());
//
//        ticketMessageRepository.save(ticketMessage);
//        System.out.println("✅ Saved latest reply for Ticket ID: " + ticketId);
//    }
//
//// this below method is being use for sending acknowledgement mail to recepient when message added from
//    // portal in message section then this method is used.
//    public void sendAcknowledgmentReplyToTicket(Long ticketId, String messageContent, String inReplyToMessageId) {
//        Ticket ticket = ticketRepository.findById(ticketId)
//                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
//
//        // Get the currently logged-in user
//        User sender = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
//                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
//
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//
//            String recipientEmail;
//            List<String> ccEmail = null;
//
//            // Determine who should be in "To" and who should be in "Cc"
//            if (sender.getId().equals(ticket.getEmployee().getId())) {
//                // Sender is the employee
//                if (ticket.getAssignee() != null) {
//                    recipientEmail = ticket.getAssignee().getEmail(); // Send to assignee
//                    ccEmail = ticket.getCcEmails(); // CC the employee
//                } else {
//                    recipientEmail = "neerajcmb@gmail.com"; // Default recipient
//                }
//            } else if (ticket.getAssignee() != null && sender.getId().equals(ticket.getAssignee().getId())) {
//                // Sender is the assignee
//                recipientEmail = ticket.getEmployee().getEmail(); // Send to employee
//                ccEmail = ticket.getCcEmails(); // CC the assignee
//            } else {
//                // Sender is neither the employee nor assignee — fallback to manager
//                recipientEmail = "kumarneerajkumar1781@gmail.com";
//            }
//
//            helper.setTo(recipientEmail);
//
//            if (ccEmail != null) {
//                helper.setCc(ccEmail.toArray(new String[0]));
//            }
//
//            helper.setSubject("Re: Ticket ID: " + ticket.getId() + " - " + ticket.getTitle());
//            helper.setText("Your message has been received:\n\n" + messageContent, false);
//
//            // Email threading headers
//            if (inReplyToMessageId != null) {
//                message.setHeader("In-Reply-To", inReplyToMessageId);
//                message.setHeader("References", inReplyToMessageId);
//            }
//
//            message.saveChanges(); // Required for Message-ID
//            String messageId = message.getMessageID();
//
//            mailSender.send(message);
//
//            System.out.printf("✅ Acknowledgment reply sent to %s%s%n",
//                    recipientEmail, ccEmail != null ? " (cc: " + ccEmail + ")" : "");
//
//        } catch (Exception e) {
//            System.err.println("❌ Failed to send acknowledgment reply for ticket ID: " + ticketId);
//            e.printStackTrace();
//        }
//    }
//
//    public void sendTicketAcknowledgmentEmail(
//            String recipientEmail,
//            Ticket ticket,
//            @Nullable List<String> ccEmails,
//            @Nullable String inReplyToMessageId,
//            @Nullable String originalSubject
//    ) {
//        try {
//            MimeMessage message = mailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//
//            // Subject - add ticket ID and fallback to title
//            String subject = "Ticket ID: " + ticket.getId() + " - " +
//                    (originalSubject != null ? originalSubject : ticket.getTitle());
//
//            helper.setTo(recipientEmail);
//            helper.setSubject(subject);
//
//            // Optional CCs
//            if (ccEmails != null && !ccEmails.isEmpty()) {
//                helper.setCc(ccEmails.toArray(new String[0]));
//            }
//
//            // HTML email content
//            String htmlContent = """
//                <html>
//                    <body style="font-family: Arial, sans-serif;">
//                        <p>Dear User,</p>
//                        <p>Your support ticket has been created successfully with the following details:</p>
//                        <table style="border-collapse: collapse; width: 100%%;">
//                            <tr><td><strong>Ticket ID:</strong></td><td>%d</td></tr>
//                            <tr><td><strong>Description:</strong></td><td>%s</td></tr>
//                            <tr><td><strong>Status:</strong></td><td>%s</td></tr>
//                            <tr><td><strong>Created At:</strong></td><td>%s</td></tr>
//                        </table>
//                        <p>We will get back to you shortly.</p>
//                        <br>
//                        <p>Best Regards,<br><strong>IT Support Team</strong></p>
//                    </body>
//                </html>
//                """.formatted(
//                    ticket.getId(),
//                    ticket.getDescription(),
//                    ticket.getStatus().name(),
//                    ticket.getCreatedAt().toString()
//            );
//
//            helper.setText(htmlContent, true); // HTML enabled
//
//            // Threading headers for reply
//            if (inReplyToMessageId != null) {
//                message.setHeader("In-Reply-To", inReplyToMessageId);
//                message.setHeader("References", inReplyToMessageId);
//            }
//
//// Generate and store new Message-ID
//            message.saveChanges(); // Required for getMessageID()
//            String newMessageId = message.getMessageID();
//            ticket.setMessageId(newMessageId); // Save for future threading
//            ticketRepository.save(ticket);
//
//            mailSender.send(message);
//            System.out.println("✅ Acknowledgment email sent to " + recipientEmail);
//            System.out.println("your message id "+ticket.getMessageId());
//
//        } catch (Exception e) {
//            System.err.println("❌ Failed to send acknowledgment email to " + recipientEmail);
//            e.printStackTrace();
//        }
//    }
//
//}
