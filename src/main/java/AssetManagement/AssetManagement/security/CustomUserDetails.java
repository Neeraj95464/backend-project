//package AssetManagement.AssetManagement.security;
//
//import AssetManagement.AssetManagement.entity.User;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.util.Collection;
//import java.util.Collections;
//
//public class CustomUserDetails implements UserDetails {
//    private final User user;
//
//    public CustomUserDetails(User user) {
//        if (user == null) {
//            throw new IllegalArgumentException("User cannot be null");
//        }
//        this.user = user;
//    }
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        if (user.getRole() == null || user.getRole().isEmpty()) {
//            throw new IllegalStateException("User role cannot be null or empty"); // 🚀 Ensure role is assigned
//        }
//        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase()));
//    }
//
//    @Override
//    public String getPassword() {
//        return user.getPassword();
//    }
//
//    @Override
//    public String getUsername() {
//        return user.getEmployeeId();  // ✅ Use empId instead of username
//    }
//
//    @Override
//    public boolean isAccountNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return true;
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return true;
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return true;
//    }
//}


package AssetManagement.AssetManagement.security;

import AssetManagement.AssetManagement.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {
    private final User user;

    public CustomUserDetails(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.user = user;

        // 🔍 Debug: Print user initialization
//        System.out.println("[DEBUG] CustomUserDetails initialized for employeeId: " + user.getEmployeeId());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = user.getRole();
        if (role == null || role.isEmpty()) {
            throw new IllegalStateException("User role cannot be null or empty");
        }

        String authority = "ROLE_" + role.toUpperCase();
//        System.out.println("[DEBUG] Assigned authority: " + authority);
        return Collections.singletonList(new SimpleGrantedAuthority(authority));
    }

    @Override
    public String getPassword() {
//        System.out.println("[DEBUG] Returning password hash for employeeId: " + user.getEmployeeId());
        return user.getPassword();  // Hashed password
    }

    @Override
    public String getUsername() {
//        System.out.println("[DEBUG] Returning username (employeeId): " + user.getEmployeeId());
        return user.getEmployeeId();  // Used in authentication
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
