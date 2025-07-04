package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.ApiResponse;
import AssetManagement.AssetManagement.dto.AuthRequest;
import AssetManagement.AssetManagement.dto.ChangePasswordRequest;
import AssetManagement.AssetManagement.dto.UserDTO;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.exception.BadRequestException;
import AssetManagement.AssetManagement.exception.UserNotFoundException;
import AssetManagement.AssetManagement.repository.UserRepository;
import AssetManagement.AssetManagement.security.JwtTokenProvider;
import AssetManagement.AssetManagement.security.impl.UserDetailsServiceImpl;
import AssetManagement.AssetManagement.util.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest authRequest) {
        System.out.println("Login request: " + authRequest);

        Optional<User> optionalUser = userRepository.findByEmployeeId(authRequest.getEmployeeId());
        if (optionalUser.isEmpty()) {
//            System.out.println("No user found with employeeId: " + authRequest.getEmployeeId());
            throw new UsernameNotFoundException("User not found with employeeId: " + authRequest.getEmployeeId());
        }
        User user = optionalUser.get();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmployeeId(), authRequest.getPassword())
            );
//            System.out.println("Authentication success: " + authentication);
        } catch (Exception ex) {
//            System.out.println("Authentication failed: " + ex.getMessage());
            throw ex; // Or return 401
        }

        String token = jwtTokenProvider.generateToken(authentication, authRequest.getEmployeeId());
//        System.out.println("Generated token: " + token);

        UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmployeeId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("empId", authRequest.getEmployeeId());
        response.put("roles", userDetails.getAuthorities());

        return response;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody UserDTO request) {
        if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new BadRequestException("Employee ID already exists.");
        }

//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new BadRequestException("Email already registered.");
//        }

        String userEmpId = AuthUtils.getAuthenticatedUsername();
        User isNormalUser= userRepository.findByEmployeeId(userEmpId)
                .orElseThrow(() ->new UserNotFoundException("user not found "+userEmpId));
        if(isNormalUser.getRole().equals("USER")){
            return ResponseEntity.ok(new ApiResponse<>(
                    false, "You don't have permission to register user", null));
        }

        String defaultUserPassword="test";
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(defaultUserPassword));
        user.setEmployeeId(request.getEmployeeId());
        user.setRole(request.getRole());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDepartment(request.getDepartment());
        user.setNote(request.getNote());
        user.setCreatedBy(AuthUtils.getAuthenticatedUserExactName());
        user.setLocation(request.getLocation());
        user.setSite(request.getSite());

        userRepository.save(user);

        return ResponseEntity.ok(new ApiResponse<>(true, "User registered successfully", null));
    }


@PutMapping("/change-password")
public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordRequest request) {
    Map<String, String> response = new HashMap<>();

    if (request.getOldPassword() == null || request.getNewPassword() == null) {
        response.put("message", "Old password and new password must be provided.");
        return ResponseEntity.badRequest().body(response);
    }

    try {
        changePassword(request.getOldPassword(), request.getNewPassword());
        response.put("message", "Password changed successfully.");
        return ResponseEntity.ok(response);

    } catch (Exception e) {
        response.put("message", "An error occurred while changing the password.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

public void changePassword( String oldPassword, String newPassword) {
    String userId = AuthUtils.getAuthenticatedUsername();
    User user = userRepository.findByEmployeeId(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
        throw new RuntimeException("Old password is incorrect");
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
}
}
//@RestController
//@RequestMapping("/api/auth")
//public class AuthController {
//
//    @Autowired
//    private AuthenticationManager authenticationManager;
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private JwtTokenProvider jwtTokenProvider;
//
//    @Autowired
//    private UserDetailsServiceImpl userDetailsService;
//
//    @PostMapping("/login")
//    public Map<String, Object> login(@RequestBody AuthRequest authRequest) {
//        System.out.println("Login request: " + authRequest);
//
//        // Authenticate user by empId and password
//        Authentication authentication = authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(authRequest.getEmployeeId(), authRequest.getPassword())
//        );
//
//        // Generate JWT Token with empId
//        String token = jwtTokenProvider.generateToken(authentication, authRequest.getEmployeeId());
//
//        // Get user details using empId
//        UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmployeeId());
//
//        // Response with token and user info
//        Map<String, Object> response = new HashMap<>();
//        response.put("token", token);
//        response.put("empId", authRequest.getEmployeeId());
//        response.put("roles", userDetails.getAuthorities());
//
//        return response;
//    }
//
//
//    @PostMapping("/register")
//    public Map<String, Object> register(@RequestBody UserDTO request) {
//        Map<String, Object> response = new HashMap<>();
//        String creatingUser= AuthUtils.getAuthenticatedUserExactName();
//
//        System.out.println("request received with "+request);
//
//        if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
//            response.put("success", false);
//            response.put("message", "Employee ID already exists.");
//            return response;
//        }
//
//        if (userRepository.existsByEmail(request.getEmail())) {
//            response.put("success", false);
//            response.put("message", "Email already registered.");
//            return response;
//        }
//        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//
//        User user = new User();
//        user.setUsername(request.getUsername());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setEmployeeId(request.getEmployeeId());
//        user.setRole(request.getRole());
//        user.setEmail(request.getEmail());
//        user.setPhoneNumber(request.getPhoneNumber());
//        user.setDepartment(request.getDepartment());
//        user.setNote(request.getNote());
//        user.setCreatedBy(creatingUser); // Or fetch from current user context
//
//        // Set site and location (assumes frontend sends complete objects or at least ID)
//        user.setLocation(request.getLocation());
//        user.setSite(request.getSite());
//
//        userRepository.save(user);
//
//        response.put("success", true);
//        response.put("message", "User registered successfully.");
//        return response;
//    }
//}
