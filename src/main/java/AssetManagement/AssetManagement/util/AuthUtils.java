package AssetManagement.AssetManagement.util;

import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtils {

    private static UserRepository userRepository;

    // ✅ Inject UserRepository (Spring manages this bean)
    public AuthUtils(UserRepository userRepository) {
        AuthUtils.userRepository = userRepository;
    }

    // ✅ Get authenticated username (employee ID)
    public static String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return "Unknown User";
    }

    // ✅ Get the exact employee name
    public static String getAuthenticatedUserExactName() {
        String employeeId = getAuthenticatedUsername();
        if (employeeId.equals("Unknown User")) {
            return "Unknown User";
        }

        return userRepository.findByEmployeeId(employeeId)
                .map(User::getUsername) // ✅ Assuming `getFullName()` returns the actual employee name
                .orElse("Unknown User"); // ✅ Handle case where user is not found
    }
}
