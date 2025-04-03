package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.AuthRequest;
import AssetManagement.AssetManagement.security.JwtTokenProvider;
import AssetManagement.AssetManagement.security.impl.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

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
}
