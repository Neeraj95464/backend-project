package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.ChangePasswordRequest;
import AssetManagement.AssetManagement.dto.PaginatedResponse;
import AssetManagement.AssetManagement.dto.UserDTO;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Get all users
    @GetMapping
    public ResponseEntity<PaginatedResponse<UserDTO>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllUsers(page, size));
    }

    // Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<UserDTO>> searchUser(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PaginatedResponse<UserDTO> users = userService.searchUser(employeeId, username, phoneNumber, email, page, size);

        return ResponseEntity.ok(users);
    }

    // Create a new user
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody User user) {
        UserDTO createdUser = userService.createUser(user);
        return ResponseEntity.ok(createdUser);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<UserDTO>> createUsers(@RequestBody List<User> users) {
        List<UserDTO> createdUsers = userService.createUsers(users);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUsers);
    }

    // Update an existing user
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        if (updatedUser != null) {
            return ResponseEntity.ok(updatedUser);  // Return 200 OK with updated UserDTO
        } else {
            return ResponseEntity.notFound().build();  // Return 404 if user is not found
        }
    }
    // Delete a user by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        System.out.println("request received with id "+id);
        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build(); // 204 No Content if deleted
        } else {
            return ResponseEntity.notFound().build(); // 404 if not found
        }
    }



}