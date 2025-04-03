package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.TicketCategory;
import AssetManagement.AssetManagement.dto.TicketDTO;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.TicketMessage;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.TicketStatus;
import AssetManagement.AssetManagement.mapper.TicketMapper;
import AssetManagement.AssetManagement.repository.TicketMessageRepository;
import AssetManagement.AssetManagement.repository.TicketRepository;
import AssetManagement.AssetManagement.repository.UserRepository;
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
                        sendAcknowledgmentEmail(mimeMessage, ticketDTO, ccEmails); // Send reply as acknowledgment
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

    @Transactional
    public TicketDTO createTicketFromEmail(String emailSubject, String emailBody, String senderEmail) {
        Ticket ticket = new Ticket();
        ticket.setTitle(emailSubject); // Email subject as ticket title
        ticket.setDescription(emailBody); // Email body as ticket description
        ticket.setCategory(TicketCategory.OTHER); // Default category (update as needed)
        ticket.setStatus(TicketStatus.OPEN);

        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        Ticket savedTicket = ticketRepository.save(ticket);
        return ticketMapper.toDTO(savedTicket);
    }

    public void sendTicketCreationEmail(String userEmail, Ticket ticket) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(userEmail);
            helper.setSubject("Ticket ID: " + ticket.getId() + " - " + ticket.getTitle());

            String emailBody = "<html><body>"
                    + "<p>Hello,</p>"
                    + "<p>Your ticket has been successfully created.</p>"
                    + "<p><b>Ticket ID:</b> " + ticket.getId() + "</p>"
                    + "<p><b>Title:</b> " + ticket.getTitle() + "</p>"
                    + "<p><b>Status:</b> " + ticket.getStatus() + "</p>"
                    + "<p><b>Created At:</b> " + ticket.getCreatedAt() + "</p>"
                    + "<p>We will get back to you shortly.</p>"
                    + "<p>Best Regards,<br>IT Support Team</p>"
                    + "</body></html>";

            helper.setText(emailBody, true); // ✅ Enables HTML content

            mailSender.send(message);
            System.out.println("✅ Ticket creation email sent to: " + userEmail);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        // ✅ Fetch the latest ticket message (last email in the thread)
        TicketMessage lastMessage = ticketMessageRepository.findTopByTicketOrderBySentAtDesc(ticket);

        if (lastMessage == null) {
            System.out.println("⚠ No previous messages found for ticket: " + ticketId);
            return;
        }

        String recipient = ticket.getEmployee().getEmail(); // Send reply to ticket creator

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipient);
            helper.setSubject(ticket.getTitle());
            helper.setText("New update on your ticket:\n\n" + messageContent, false);

            // ✅ Ensure the reply is in the same thread
            if (lastMessage.getMessageId() != null) {
                message.setHeader("In-Reply-To", lastMessage.getMessageId());
                message.setHeader("References", lastMessage.getMessageId());
            }

            message.saveChanges(); // ✅ Ensures Message-ID is generated

            mailSender.send(message);
            System.out.println("✅ Reply sent for Ticket ID: " + ticketId);

            // ✅ Save the sent message in the database
            TicketMessage replyMessage = new TicketMessage();
            replyMessage.setTicket(ticket);
            replyMessage.setSender(ticket.getEmployee()); // Assign sender
            replyMessage.setMessage(messageContent);
            replyMessage.setSentAt(LocalDateTime.now());
            replyMessage.setMessageId(message.getMessageID()); // ✅ Now this will be non-null

            ticketMessageRepository.save(replyMessage);

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


    // ✅ Sends an acknowledgment email as a REPLY to the original ticket email
    private void sendAcknowledgmentEmail(Message originalEmail, TicketDTO ticket,List<String> ccEmails) {
        try {
            MimeMessage replyMessage = (MimeMessage) mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(replyMessage, true);

            String senderEmail = ((InternetAddress) originalEmail.getFrom()[0]).getAddress();
            String messageId = originalEmail.getHeader("Message-ID")[0]; // Get original message ID

            // ✅ Modify subject to include Ticket ID
            String newSubject = "Ticket ID: " + ticket.getId() + " - " + originalEmail.getSubject();


            helper.setTo(senderEmail);
            helper.setSubject(newSubject); // Updated subject

            if (!ccEmails.isEmpty()) {
                helper.setCc(ccEmails.toArray(new String[0]));
            }
            helper.setText(
                    "Hello,\n\n" +
                            "Your ticket has been created successfully.\n\n" +
                            "**Ticket ID:** " + ticket.getId() + "\n" +
                            "**Title:** " + ticket.getTitle() + "\n" +
                            "**Status:** " + ticket.getStatus() + "\n\n" +
                            "We will get back to you shortly.\n\n" +
                            "Best Regards,\nIT Support Team"
            );

            // ✅ Ensure email threads correctly
            replyMessage.setHeader("In-Reply-To", messageId);
            replyMessage.setHeader("References", messageId);

            mailSender.send(replyMessage);
            System.out.println("✅ Acknowledgment reply sent to: " + senderEmail + " (CC: " + ccEmails + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
