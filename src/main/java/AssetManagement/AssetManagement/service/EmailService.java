package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.TicketDTO;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketMessage;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.TicketCategory;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import AssetManagement.AssetManagement.enums.TicketMessageType;
import AssetManagement.AssetManagement.enums.TicketStatus;
import AssetManagement.AssetManagement.exception.UserNotFoundException;
import AssetManagement.AssetManagement.mapper.TicketMapper;
import AssetManagement.AssetManagement.repository.TicketMessageRepository;
import AssetManagement.AssetManagement.repository.TicketRepository;
import AssetManagement.AssetManagement.repository.UserRepository;
import AssetManagement.AssetManagement.util.AuthUtils;
import AssetManagement.AssetManagement.util.GraphClientProvider;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;
import jakarta.annotation.Nullable;
import jakarta.mail.Address;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.GraphServiceClient;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.persistence.EntityNotFoundException;
import okhttp3.Request;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailService {

    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private TicketMapper ticketMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TicketMessageRepository ticketMessageRepository;
    private static final List<String> COMPANY_DOMAINS = List.of(
            "@mahavirauto.co",
            "@mahavirgroup.co",
            "@mahavirauto.in",
            "@mahavirauto.com",
            "@mahavirgroup.com",
            "@mahavirmotors.com",
            "benelli-india.com",
            "keeway-india.com",
            "coastalstar.in",
            "benelli-hyderabad.com",
            "zontes-india.com",
            "qjmotor-india.com",
            "motomorini-india.com",
            "mahavirisuzu.com",
            "mahavirhyd.in",
            "mahavirauto.in"
    );


//    @Autowired
//    private JavaMailSender mailSender; // Inject Spring Mail Sender

    private final GraphServiceClient<Request> graphClient;

    public EmailService(GraphClientProvider graphClientProvider) {
        this.graphClient = graphClientProvider.getGraphClient();
    }


    public void sendEmailViaGraph(String toEmail, String subject, String bodyHtml, List<String> ccEmails) {
        Recipient toRecipient = new Recipient();
        EmailAddress emailAddress = new EmailAddress();
        emailAddress.address = toEmail;
        toRecipient.emailAddress = emailAddress;

        List<Recipient> ccRecipients = new ArrayList<>();
        if (ccEmails != null) {
            for (String ccEmail : ccEmails) {
                Recipient ccRecipient = new Recipient();
                EmailAddress ccAddress = new EmailAddress();
                ccAddress.address = ccEmail;
                ccRecipient.emailAddress = ccAddress;
                ccRecipients.add(ccRecipient);
            }
        }

        Message message = new Message();
        message.subject = subject;

        ItemBody body = new ItemBody();
        body.contentType = BodyType.HTML;
        body.content = bodyHtml;
        message.body = body;

        message.toRecipients = List.of(toRecipient);
        message.ccRecipients = ccRecipients;

        graphClient
                .users("support@mahavirgroup.co")
                .sendMail(UserSendMailParameterSet
                        .newBuilder()
                        .withMessage(message)
                        .withSaveToSentItems(true)
                        .build())
                .buildRequest()
                .post();

        System.out.println("✅ Email sent to " + toEmail + " via Graph API");
    }

    @Scheduled(fixedRate = 60000)
    public void checkEmailsForTickets() {
        String mailboxUser = "support@mahavirgroup.co";

        try {
            MessageCollectionPage messages = graphClient
                    .users(mailboxUser)
                    .messages()
                    .buildRequest()
                    .select("subject,from,body,isRead,internetMessageHeaders")
                    .top(20)
                    .get();

            List<Message> messageList = messages.getCurrentPage();

            for (Message message : messageList) {
                if (Boolean.TRUE.equals(message.isRead)) {
                    continue; // Skip already processed emails
                }

                String subject = message.subject;
                String senderEmail = message.from.emailAddress.address;
//                String content = message.body.content;

                String htmlContent = message.body.content;
                String content = Jsoup.parse(htmlContent).text();



                List<String> ccEmails = extractCcRecipients(message); // Custom method to extract CCs
                String messageId = extractHeaderValue(message, "Message-ID");

                System.out.println("Received Email - Subject: " + subject);
//                System.out.println("Content: " + content);
                System.out.println("From: " + senderEmail);
                System.out.println("CC Recipients: " + ccEmails);

                try {
                    Long ticketId = extractTicketIdFromSubject(subject);
                    if (ticketId != null) {
                        saveReplyToTicket(ticketId, senderEmail, content, ccEmails);
                    } else {
                        TicketDTO ticketDTO = createTicketFromEmail(subject, content, senderEmail);
                        sendTicketAcknowledgmentEmail(
                                senderEmail,
                                ticketRepository.findById(ticketDTO.getId()).orElseThrow(),
                                ccEmails,
                                null,
                                null
//                                messageId,
//                                subject
                        );
                    }

                    // Mark as read
                    Message messageUpdate = new Message();
                    messageUpdate.isRead = true;
                    graphClient.users(mailboxUser)
                            .messages(message.id)
                            .buildRequest()
                            .patch(messageUpdate);

                } catch (Exception processingEx) {
                    System.err.println("Failed to process email: " + processingEx.getMessage());
                    processingEx.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.err.println("Error during email polling: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private String extractHeaderValue(Message message, String headerName) {
        if (message.internetMessageHeaders == null) return null;
        return message.internetMessageHeaders.stream()
                .filter(h -> headerName.equalsIgnoreCase(h.name))
                .map(h -> h.value)
                .findFirst()
                .orElse(null);
    }

    private List<String> extractCcRecipients(Message message) {
        if (message.ccRecipients == null) return List.of();
        return message.ccRecipients.stream()
                .map(recipient -> recipient.emailAddress.address)
                .toList();
    }


//    @Transactional
//    public TicketDTO createTicketFromEmail(String emailSubject, String emailBody, String senderEmail) {
//        Ticket ticket = new Ticket();
//        ticket.setTitle(emailSubject);
//        ticket.setDescription(emailBody);
//        ticket.setCategory(TicketCategory.OTHER);
////        ticket.setTicketDepartment(null);
//        ticket.setTicketDepartment(TicketDepartment.IT);
////        ticket.setStatus(TicketStatus.OPEN);
//
//        System.out.println("Sender email: " + senderEmail);
//
////        User sender = userRepository.findByEmail(senderEmail)
////                .orElseThrow(() -> new UserNotFoundException("User not found"));
//
//        List<User> matchedUsers = userRepository.findAllByEmail(senderEmail);
//
//        User sender;
//
//        if (matchedUsers.size() == 1) {
//            // ✅ Exactly one user found
//            sender = matchedUsers.getFirst();
//        } else if (matchedUsers.isEmpty()) {
//            // ❌ No user found, check company domain
//            boolean domainMatched = COMPANY_DOMAINS.stream().anyMatch(senderEmail::endsWith);
//
//            if (domainMatched) {
//                // ✅ Domain matched, create fallback user
//                sender = new User();
//                sender.setUsername("Unknown User");
//                sender.setEmail(senderEmail);
//                sender.setNote("User not found in DB but domain matched");
//
//                String nextTempEmpId = generateNextTempEmployeeId();
//                sender.setEmployeeId(nextTempEmpId);
//            } else {
//                // ❌ Not allowed to raise ticket if domain doesn't match
//                throw new RuntimeException("Unauthorized sender: Email domain not allowed.");
//            }
//
//        } else {
//            // ❌ Multiple users found
////            sender = new User();
////            sender.setUsername("Multiple Users Found");
////            sender.setEmail(senderEmail);
////            sender.setNote("Multiple users found with the same email");
//
//            // Generate unique temp employeeId
//            String nextTempEmpId = generateNextTempEmployeeId();
//            sender.setEmployeeId(nextTempEmpId);
//        }
//
//        userRepository.save(sender);
//
//        System.out.println("User found - Email: " + sender.getUsername());
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

    @Transactional
    public TicketDTO createTicketFromEmail(String emailSubject, String emailBody, String senderEmail) {

        Ticket ticket = new Ticket();
        ticket.setTitle(emailSubject);
        ticket.setDescription(emailBody);
        ticket.setCategory(TicketCategory.OTHER);
        ticket.setTicketDepartment(TicketDepartment.IT);

//        System.out.println("Sender email: " + senderEmail);

        List<User> matchedUsers = userRepository.findAllByEmail(senderEmail);

        User sender;

        if (matchedUsers.size() == 1) {
            // ✅ Exactly one user found
            sender = matchedUsers.get(0);

        } else if (matchedUsers.isEmpty()) {
            // ❌ No user found, check company domain
            boolean domainMatched = COMPANY_DOMAINS
                    .stream()
                    .anyMatch(senderEmail::endsWith);

            if (domainMatched) {
                // ✅ Domain matched → create fallback user
                sender = new User();
                sender.setUsername("Unknown User");
                sender.setEmail(senderEmail);
                sender.setNote("User not found in DB but domain matched");

                String nextTempEmpId = generateNextTempEmployeeId();
                sender.setEmployeeId(nextTempEmpId);

                userRepository.save(sender);
            } else {
                // ❌ Domain not allowed
                throw new RuntimeException("Unauthorized sender: Email domain not allowed.");
            }

        } else {
            // ✅ Multiple users found → pick the latest one
            sender = matchedUsers.stream()
                    .max(Comparator.comparing(User::getId))
                    .orElseThrow(() ->
                            new RuntimeException("Unable to resolve user from multiple matches"));
        }

        System.out.println("User resolved - Username: " + sender.getUsername());

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


    private String generateNextTempEmployeeId() {
        List<String> tempEmpIds = userRepository.findAllEmployeeIdsStartingWith("temp");

        int max = 0;
        for (String empId : tempEmpIds) {
            try {
                int num = Integer.parseInt(empId.replace("temp", ""));
                if (num > max) max = num;
            } catch (NumberFormatException ignored) {
            }
        }

        return "temp" + (max + 1);
    }



    private List<String> extractRecipients(jakarta.mail.Message message) throws MessagingException {
        List<String> recipients = new ArrayList<>();

        Address[] toAddresses = message.getRecipients(jakarta.mail.Message.RecipientType.TO);
        Address[] ccAddresses = message.getRecipients(jakarta.mail.Message.RecipientType.CC);

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
    private String extractTextFromMessage(jakarta.mail.Message message) throws Exception {
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

//private String extractLatestReply(String fullMessage) {
//    if (fullMessage == null || fullMessage.isBlank()) return "";
//
//    String[] lines = fullMessage.split("\\r?\\n");
//    StringBuilder latestReply = new StringBuilder();
//
//    for (String rawLine : lines) {
//        String line = rawLine.trim().toLowerCase();
//
//        // Stop if this line indicates beginning of quoted email
//        if (line.startsWith("from:") || line.startsWith("sent:") ||
//            line.startsWith("subject:") || line.startsWith("to:") ||
//            line.contains("wrote:") || (line.startsWith("on ") && line.contains("wrote:"))) {
//            break;
//        }
//
//        latestReply.append(rawLine).append("\n");
//    }
//
//    return latestReply.toString().trim();
//}

    private String extractLatestReply(String fullMessage) {
        if (fullMessage == null || fullMessage.isBlank()) return "";

        // Normalize line breaks for consistency
        String normalized = fullMessage.replaceAll("\\r\\n|\\r", "\n");

        // Try to detect start of previous email thread using regex (non-greedy)
        String[] splitMarkers = {
                "From:", "Sent:", "Subject:", "To:", "Cc:", "On .* wrote:"
        };

        for (String marker : splitMarkers) {
            Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(marker)); // case-insensitive
            Matcher matcher = pattern.matcher(normalized);

            if (matcher.find()) {
                // Return content before this marker
                return normalized.substring(0, matcher.start()).trim();
            }
        }

        // Fallback: return entire trimmed message
        return normalized.trim();
    }


    public void sendReplyToTicket(Long ticketId, String messageContent) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));

        TicketMessage firstMessage = ticketMessageRepository.findTopByTicketOrderBySentAtAsc(ticket);
        String recipient = ticket.getEmployee().getEmail();

        String subject = "Re: Ticket ID: " + ticket.getId() + " - " + ticket.getTitle();
//        String threadId = firstMessage != null ? firstMessage.getMessageId() : null;

//        graphClient.users("user@example.com").messages("{message-id}").createReply().buildRequest().post()

        // Use Graph API instead of JavaMailSender
        sendEmailViaGraph(
                recipient,
                subject,
                "<p>New update on your ticket:</p><p>" + messageContent + "</p>",
                ticket.getCcEmails()// optional cc
//                threadId
        );

        // Save message
        TicketMessage replyMessage = new TicketMessage();
        replyMessage.setTicket(ticket);
        replyMessage.setSender(ticket.getEmployee()); // or logged-in user
        replyMessage.setMessage(messageContent);
        replyMessage.setSentAt(LocalDateTime.now());

        ticketMessageRepository.save(replyMessage);

        System.out.println("✅ Reply sent for Ticket ID: " + ticketId + " via Graph API");
    }

    // this method is called when we are sending mail in already created ticket to add like message.
    private void saveReplyToTicket(Long ticketId, String senderEmail, String messageContent,List<String> ccEmails) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found in save reply to ticket method with ticket id is "+ticketId));

//        User sender = userRepository.findByEmail(senderEmail)
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));

//        User sender = ticket.getEmployee();
        List<User> matchedUsers = userRepository.findAllByEmail(senderEmail);

        User sender;

        if (matchedUsers.size() == 1) {
            // ✅ Exactly one user found
            sender = matchedUsers.getFirst();
        } else if (matchedUsers.isEmpty()) {
            // ❌ No user found, check company domain
            boolean domainMatched = COMPANY_DOMAINS.stream().anyMatch(senderEmail::endsWith);

            if (domainMatched) {
                // ✅ Domain matched, create fallback user
                sender = new User();
                sender.setUsername("Unknown User");
                sender.setEmail(senderEmail);
                sender.setNote("User not found in DB but domain matched");

                String nextTempEmpId = generateNextTempEmployeeId();
                sender.setEmployeeId(nextTempEmpId);
            } else {
                // ❌ Not allowed to raise ticket if domain doesn't match
                throw new RuntimeException("Unauthorized sender: Email domain not allowed.");
            }

        } else {
            // ❌ Multiple users found
            sender = new User();
            sender.setUsername("Multiple Users Found");
            sender.setEmail(senderEmail);
            sender.setNote("Multiple users found with the same email");

            // Generate unique temp employeeId
            String nextTempEmpId = generateNextTempEmployeeId();
            sender.setEmployeeId(nextTempEmpId);
        }

        userRepository.save(sender);
        String recipient;

        if(sender == ticket.getEmployee()){
            recipient = ticket.getAssignee().getEmail();
        } else if (sender == ticket.getAssignee()) {

        }

        // ✅ Extract only the latest reply
        String latestReply = extractLatestReply(messageContent);
        System.out.println("Mail reply received ticket id and message is "+ticketId +" "+latestReply);

        // ✅ Ensure message is within a safe length limit
        if (latestReply.length() > 5000) {
            latestReply = latestReply.substring(0, 5000) + "...";
        }

        TicketMessage ticketMessage = new TicketMessage();
        ticketMessage.setTicket(ticket);
        ticketMessage.setSender(sender);
        ticketMessage.setMessage(latestReply);
        ticketMessage.setSentAt(LocalDateTime.now());
        ticketMessage.setTicketMessageType(TicketMessageType.PUBLIC_RESPONSE);

        ticketMessageRepository.save(ticketMessage);
        System.out.println("✅ Saved latest reply for Ticket ID: " + ticketId);

//        sendEmailViaGraph(
//                recipient,
//                "Ticket ID: " + ticket.getId() + " - " +
//                        (originalSubject != null ? originalSubject : ticket.getTitle());,
//                "<p>New update on your ticket:</p><p>" + messageContent + "</p>",
//                ticket.getCcEmails()// optional cc
////                threadId
//        );
    }

    public void sendInternalMail(String senderEmail, String messagePreview, List<String> ccEmails,Ticket ticket) {
        // Build TO recipient (sender)
        Recipient toRecipient = new Recipient();
        EmailAddress senderAddress = new EmailAddress();
        senderAddress.address = senderEmail;
        toRecipient.emailAddress = senderAddress;

        // Build CC recipients
        List<Recipient> ccRecipients = new ArrayList<>();
        if (ccEmails != null && !ccEmails.isEmpty()) {
            for (String ccEmail : ccEmails) {
                Recipient ccRecipient = new Recipient();
                EmailAddress ccAddress = new EmailAddress();
                ccAddress.address = ccEmail;
                ccRecipient.emailAddress = ccAddress;
                ccRecipients.add(ccRecipient);
            }
        }

        // Attractive HTML body
        String bodyHtml = """
        <html>
            <body style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px;">
                <div style="max-width: 600px; margin: auto; background-color: #ffffff; padding: 20px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1);">
                    <h2 style="color: #004aad;">📩 Internal Note Added in Ticket </h2>
                    <p style="font-size: 16px; color: #333333;">
                        %s
                    </p>
                    <hr style="margin: 20px 0; border: none; border-top: 1px solid #ddd;">
                    <p style="font-size: 14px; color: #888888;">
                        Please do not reply to this email directly. For more info, visit your ticket portal.
                    </p>
                </div>
            </body>
        </html>
    """.formatted(messagePreview);

        // Create and send message
        Message message = new Message();
//        message.subject = "Ticket Notification";  // You can hardcode or make dynamic
         message.subject = "Re: Ticket ID: " + ticket.getId() + " - " + ticket.getTitle();
        ItemBody body = new ItemBody();
        body.contentType = BodyType.HTML;
        body.content = bodyHtml;
        message.body = body;
        message.toRecipients = List.of(toRecipient);
        message.ccRecipients = ccRecipients;

        graphClient
                .users("support@mahavirgroup.co")
                .sendMail(UserSendMailParameterSet
                        .newBuilder()
                        .withMessage(message)
                        .withSaveToSentItems(true)
                        .build())
                .buildRequest()
                .post();

        System.out.println("✅ Styled email sent to " + senderEmail + " with CC: " + ccEmails);
    }

    public void sendAcknowledgmentReplyToTicket(
            Long ticketId,
            String messageContent,
            String inReplyToMessageId
    ) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));

        User sender = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));

        try {
            // Step 0: Resolve message from internetMessageId
            MessageCollectionPage results = graphClient
                    .users("support@mahavirgroup.co")
                    .messages()
                    .buildRequest()
                    .filter("internetMessageId eq '" + inReplyToMessageId + "'")
                    .get();

            List<Message> messages = results.getCurrentPage();
            if (messages.isEmpty()) {
                System.err.println("❌ No message found with internetMessageId: " + inReplyToMessageId
                        + "In ticket #"+ticketId);
                return;
            }

            String messageId = messages.get(0).id;

            // Step 1: Create a reply draft
            Message replyDraft = graphClient
                    .users("support@mahavirgroup.co")
                    .messages(messageId)
                    .createReply(MessageCreateReplyParameterSet.newBuilder().build())
                    .buildRequest()
                    .post();

            // Step 2: Determine To and CC logic
            String recipientEmail;
            Set<String> ccSet = new HashSet<>();


            if (sender.getId().equals(ticket.getEmployee().getId())) {
                // Employee is replying
                if (ticket.getAssignee() != null) {
                    recipientEmail = ticket.getAssignee().getEmail();
                    ccSet.add(ticket.getEmployee().getEmail());  // add employee to CC
                } else {

                    // when no one is assignee (might be ticket created by mail so after followup maill
                    // will go on this email only
                    recipientEmail = "it.manager@mahavirgroup.co";
                }
            } else if (ticket.getAssignee() != null && sender.getId().equals(ticket.getAssignee().getId())) {
                // Assignee is replying
                recipientEmail = ticket.getEmployee().getEmail();
                ccSet.add(ticket.getAssignee().getEmail());  // add assignee to CC
            } else {
                recipientEmail = "it.manager@mahavirgroup.co";
            }

            // Add previously stored CC emails
            if (ticket.getCcEmails() != null) {
                ccSet.addAll(ticket.getCcEmails());
            }

            // Remove recipient from CC
            ccSet.removeIf(email -> email.equalsIgnoreCase(recipientEmail));

            // Build recipient objects
            Recipient toRecipient = new Recipient();
            EmailAddress toEmail = new EmailAddress();
            toEmail.address = recipientEmail;
            toRecipient.emailAddress = toEmail;

            List<Recipient> ccRecipients = ccSet.stream().map(email -> {
                Recipient r = new Recipient();
                EmailAddress ea = new EmailAddress();
                ea.address = email;
                r.emailAddress = ea;
                return r;
            }).toList();

            // Step 3: Compose reply content
            Message updatedReply = new Message();

            ItemBody body = new ItemBody();
            body.contentType = BodyType.HTML;
            body.content = """
            <p>%s</p>
            <p><strong>Regards,</strong><br>%s</p>
        """.formatted(messageContent, sender.getUsername());

            updatedReply.body = body;
            updatedReply.toRecipients = List.of(toRecipient);
            if (!ccRecipients.isEmpty()) {
                updatedReply.ccRecipients = ccRecipients;
            }

            // Step 4: Patch and send
            graphClient
                    .users("support@mahavirgroup.co")
                    .messages(replyDraft.id)
                    .buildRequest()
                    .patch(updatedReply);

            graphClient
                    .users("support@mahavirgroup.co")
                    .messages(replyDraft.id)
                    .send()
                    .buildRequest()
                    .post();

            System.out.printf("✅ Acknowledgment reply sent to %s (cc: %s)%n",
                    recipientEmail, String.join(", ", ccSet));

        } catch (Exception e) {
            System.err.println("❌ Failed to send acknowledgment reply for ticket ID: " + ticketId);
            e.printStackTrace();
        }
    }





    public void sendTicketAcknowledgmentEmail(
            String recipientEmail,
            Ticket ticket,
            @Nullable List<String> ccEmails,
            @Nullable String internetMessageId,
            @Nullable String originalSubject
    ) {
//        System.out.println("trying to send by sendTicketAcknowledgmentEmail");
        try {
            String subject = "Ticket ID: " + ticket.getId() + " - " +
                    (originalSubject != null ? originalSubject : ticket.getTitle());

            String htmlContent = """
        <html>
            <body style=\"font-family: Arial, sans-serif;\">
                <p>Dear User,</p>
                <p>Your support ticket has been created successfully with the following details:</p>
                <table style=\"border-collapse: collapse; width: 100%%;\">
                    <tr><td><strong>Ticket ID:</strong></td><td>%d</td></tr>
                    <tr><td><strong>Description:</strong></td><td>%s</td></tr>
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
                    ticket.getDescription(),
                    ticket.getStatus().name(),
                    ticket.getCreatedAt().toString()
            );

            // Prepare recipient and CC
            Recipient toRecipient = new Recipient();
            EmailAddress toEmail = new EmailAddress();
            toEmail.address = recipientEmail;
            toRecipient.emailAddress = toEmail;

            List<Recipient> ccRecipients = new ArrayList<>();
            if (ccEmails != null) {
                for (String cc : ccEmails) {
                    EmailAddress ccEmail = new EmailAddress();
                    ccEmail.address = cc;
                    Recipient ccRecipient = new Recipient();
                    ccRecipient.emailAddress = ccEmail;
                    ccRecipients.add(ccRecipient);
                }
            }

            ItemBody body = new ItemBody();
            body.contentType = BodyType.HTML;
            body.content = htmlContent;

//            System.out.println("🔍 Searching for message with filter: internetMessageId eq '" + internetMessageId + "'");

            // If internetMessageId is present, search and reply to it
            if (internetMessageId != null && !internetMessageId.isEmpty()) {
                MessageCollectionPage results = graphClient
                        .users("support@mahavirgroup.co")
                        .messages()
                        .buildRequest()
                        .filter("internetMessageId eq '" + internetMessageId + "'")
                        .get();

                List<Message> messages = results.getCurrentPage();
                if (!messages.isEmpty()) {
                    Message originalMessage = messages.get(0);
                    Message replyDraft = graphClient
                            .users("support@mahavirgroup.co")
                            .messages(originalMessage.id)
                            .createReply(MessageCreateReplyParameterSet.newBuilder().build())
                            .buildRequest()
                            .post();

                    replyDraft.subject = subject;
                    replyDraft.body = body;
                    replyDraft.toRecipients = List.of(toRecipient);
                    if (!ccRecipients.isEmpty()) {
                        replyDraft.ccRecipients = ccRecipients;
                    }

                    graphClient
                            .users("support@mahavirgroup.co")
                            .messages(replyDraft.id)
                            .buildRequest()
                            .patch(replyDraft);

                    graphClient
                            .users("support@mahavirgroup.co")
                            .messages(replyDraft.id)
                            .send()
                            .buildRequest()
                            .post();

                    ticket.setMessageId(replyDraft.id);
                } else {
                    System.err.println("❌ No message found for internetMessageId: " + internetMessageId);
                }
            } else {
                // New message (not a reply)
                Message message = new Message();
                message.subject = subject;
                message.body = body;
                message.toRecipients = List.of(toRecipient);
                if (!ccRecipients.isEmpty()) {
                    message.ccRecipients = ccRecipients;
                }

                Message draft = graphClient
                        .users("support@mahavirgroup.co")
                        .messages()
                        .buildRequest()
                        .post(message);

                ticket.setMessageId(draft.id);
                ticket.setInternetMessageId(draft.internetMessageId);  // <-- Add this field in Ticket entity
                ticketRepository.save(ticket);

                graphClient
                        .users("support@mahavirgroup.co")
                        .messages(draft.id)
                        .send()
                        .buildRequest()
                        .post();
            }

            ticketRepository.save(ticket);
            System.out.println("✅ Acknowledgment email sent to " + recipientEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send acknowledgment email to " + recipientEmail);
            e.printStackTrace();
        }
    }



}


