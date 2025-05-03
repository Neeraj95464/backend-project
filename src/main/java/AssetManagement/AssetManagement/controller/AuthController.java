package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.AuthRequest;
import AssetManagement.AssetManagement.dto.UserDTO;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.repository.UserRepository;
import AssetManagement.AssetManagement.security.JwtTokenProvider;
import AssetManagement.AssetManagement.security.impl.UserDetailsServiceImpl;
import AssetManagement.AssetManagement.util.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest authRequest) {
        System.out.println("Login request: " + authRequest);

        // Authenticate user by empId and password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmployeeId(), authRequest.getPassword())
        );

        // Generate JWT Token with empId
        String token = jwtTokenProvider.generateToken(authentication, authRequest.getEmployeeId());

        // Get user details using empId
        UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmployeeId());

        // Response with token and user info
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("empId", authRequest.getEmployeeId());
        response.put("roles", userDetails.getAuthorities());

        return response;
    }


    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody UserDTO request) {
        Map<String, Object> response = new HashMap<>();
        String creatingUser= AuthUtils.getAuthenticatedUserExactName();

        System.out.println("request received with "+request);

        if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
            response.put("success", false);
            response.put("message", "Employee ID already exists.");
            return response;
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            response.put("success", false);
            response.put("message", "Email already registered.");
            return response;
        }
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmployeeId(request.getEmployeeId());
        user.setRole(request.getRole());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDepartment(request.getDepartment());
        user.setNote(request.getNote());
        user.setCreatedBy(creatingUser); // Or fetch from current user context

        // Set site and location (assumes frontend sends complete objects or at least ID)
        user.setLocation(request.getLocation());
        user.setSite(request.getSite());

        userRepository.save(user);

        response.put("success", true);
        response.put("message", "User registered successfully.");
        return response;
    }
}
