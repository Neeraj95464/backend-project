package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.AuthRequest;
import AssetManagement.AssetManagement.security.JwtTokenProvider;
import AssetManagement.AssetManagement.security.impl.UserDetailsServiceImpl;
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
//@CrossOrigin(origins = "*") // Allows requests from frontend
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest authRequest) {
        System.out.println("your request was "+authRequest);

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = "$2a$10$8OOHxeZ3x/p5t3pAaD1cy./vjzpHxSTeHxPw7EjUiPF8lVUeUWaza";
        String rawPassword = authRequest.getPassword().toLowerCase();

        boolean isMatch = passwordEncoder.matches(rawPassword, hashedPassword);
        System.out.println("Password Match: " + isMatch); // Should print: true

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );

        // If authentication is successful, generate JWT token
        String token = jwtTokenProvider.generateToken(authentication);

        // Get user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());

        // Response with token and user info
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", userDetails.getUsername());
        response.put("roles", userDetails.getAuthorities());

        return response;
    }
}

