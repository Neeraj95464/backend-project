package AssetManagement.AssetManagement.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtils {
    public static String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName(); // ✅ Returns logged-in user's username
        }
        return "Unknown User"; // Fallback if user is not authenticated
    }
}

