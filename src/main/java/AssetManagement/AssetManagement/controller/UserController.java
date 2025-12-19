package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.ChangePasswordRequest;
import AssetManagement.AssetManagement.dto.PaginatedResponse;
import AssetManagement.AssetManagement.dto.UserDTO;
import AssetManagement.AssetManagement.dto.UserResponseDto;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.Department;
import AssetManagement.AssetManagement.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
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


    @PostMapping("/reset-password/{employeeId}")
    public ResponseEntity<Void> resetPassword(
            @PathVariable String employeeId
    ) {
        userService.resetPasswordByEmployeeId(employeeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/filter")
    public PaginatedResponse<UserResponseDto> filterUsers(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAfter,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return userService.filterUsers(
                employeeId,
                username,
                role,
                department,
                siteId,
                locationId,
                search,
                createdAfter,
                createdBefore,
                page,
                size
        );
    }

    @GetMapping("/filter/export")
    public void exportFilteredUsersToExcel(
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdAfter,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime createdBefore,
            HttpServletResponse response
    ) throws IOException {

        List<UserResponseDto> users = userService.filterUsersForExport(
                employeeId,
                username,
                role,
                department,
                siteId,
                locationId,
                search,
                createdAfter,
                createdBefore
        );

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = "users_" + LocalDateTime.now() + ".xlsx";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Employee ID");
            header.createCell(1).setCellValue("Username");
            header.createCell(2).setCellValue("Role");
            header.createCell(3).setCellValue("Department");
            header.createCell(4).setCellValue("Designation");
            header.createCell(5).setCellValue("Phone");
            header.createCell(6).setCellValue("Email");
            header.createCell(7).setCellValue("Site");
            header.createCell(8).setCellValue("Location");

            int rowIdx = 1;
            for (UserResponseDto u : users) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(u.getEmployeeId());
                row.createCell(1).setCellValue(u.getUsername());
                row.createCell(2).setCellValue(u.getRole() != null ? u.getRole() : "");
                row.createCell(3).setCellValue(
                        u.getDepartment() != null ? u.getDepartment().name() : ""
                );
                row.createCell(4).setCellValue(
                        u.getDesignation() != null ? u.getDesignation() : ""
                );
                row.createCell(5).setCellValue(
                        u.getPhoneNumber() != null ? u.getPhoneNumber() : ""
                );
                row.createCell(6).setCellValue(
                        u.getEmail() != null ? u.getEmail() : ""
                );
                row.createCell(7).setCellValue(
                        u.getSiteName() != null ? u.getSiteName() : ""
                );
                row.createCell(8).setCellValue(
                        u.getLocationName() != null ? u.getLocationName() : ""
                );
            }

            for (int i = 0; i <= 8; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }


}
