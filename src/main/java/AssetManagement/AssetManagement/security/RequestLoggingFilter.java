package AssetManagement.AssetManagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Path LOG_FILE = Path.of("trace.txt");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String user = "UNKNOWN";
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            user = SecurityContextHolder.getContext().getAuthentication().getName();
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }

        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip += " (localhost)";
        }

        String userAgent = request.getHeader("User-Agent");
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String timestamp = LocalDateTime.now().format(FORMATTER);

        String geoInfo = fetchGeoInfo(ip);

        String logEntry = String.format("[%s] %s %s by %s from IP: %s | Agent: %s | Location: %s%n",
                timestamp, method, uri, user, ip, userAgent, geoInfo);

        try {
            Files.writeString(LOG_FILE, logEntry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }

    private String fetchGeoInfo(String ip) {
        if (ip.contains("localhost") || ip.startsWith("0:0") || ip.startsWith("127.")) {
            return "Localhost";
        }

        try {
            URL url = new URL("https://ipapi.co/" + ip.replace(" (localhost)", "") + "/city/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                return scanner.hasNext() ? scanner.nextLine() : "Unknown";
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }
}


