package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.PaginatedResponse;
import AssetManagement.AssetManagement.dto.UserDTO;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.repository.UserRepository;
import AssetManagement.AssetManagement.util.AuthUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    public PaginatedResponse<UserDTO> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findAll(pageable);

        List<UserDTO> userDTOs = userPage.getContent().stream()
                .map(this::convertUserToDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                userDTOs,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    public PaginatedResponse<UserDTO> searchUser(String employeeId, String username, String phoneNumber, String email, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("employeeId").ascending());

        Page<User> users = userRepository.findUsers(employeeId, username, phoneNumber, email, pageable);

        List<UserDTO> userDTOList = users.getContent().stream()
                .map(this::convertUserToDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                userDTOList,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isLast()
        );
    }


    // Retrieve a user by ID
    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id).map(this::convertUserToDto);
    }

    // Create a new user
    @Transactional
    public UserDTO createUser(User user) {
        // Fetch the authenticated user's details
        Optional<User> authenticatedUser = userRepository.findByEmployeeId(AuthUtils.getAuthenticatedUsername());

        // Set the createdBy field with the name of the authenticated user
        user.setCreatedBy(authenticatedUser.map(User::getUsername).orElse("Unknown"));

        // Encode and set the password
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Assign default role
        user.setRole("USER");

        // Save the user
        User savedUser = userRepository.save(user);

        return convertUserToDto(savedUser);
    }

    public List<UserDTO> createUsers(List<User> users) {
        List<UserDTO> createdUsers = new ArrayList<>();
        for (User user : users) {
            user.setRole("USER");
            User savedUser = userRepository.save(user);
            createdUsers.add(convertUserToDto(savedUser)); // Implement convertToDTO() if not already
        }
        return createdUsers;
    }

    // Update an existing user
    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDto) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        // Update user fields
        existingUser.setUsername(userDto.getUsername());
        existingUser.setPassword(userDto.getPassword());
        existingUser.setRole(userDto.getRole());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setPhoneNumber(String.valueOf(userDto.getPhoneNumber()));
        existingUser.setDepartment(userDto.getDepartment());
        existingUser.setNote(userDto.getNote());
        existingUser.setLocation(userDto.getLocation());
        existingUser.setSite(userDto.getSite());

        User savedUser = userRepository.save(existingUser);
        return convertUserToDto(savedUser);
    }

    // Delete a user by ID
    public boolean deleteUser(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        System.out.println("in service method with user "+optionalUser);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setDeleted(true); // Set soft delete flag
            userRepository.save(user); // Save updated user
            return true;
        } else {
            return false;
        }
    }


    // Convert User entity to UserDTO

    public UserDTO convertUserToDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setDepartment(user.getDepartment());
        dto.setNote(user.getNote());
        dto.setLocation(user.getLocation());
        dto.setSite(user.getSite());

        // Ensure assigned assets are correctly mapped as a List<Asset>
        dto.setSerialNumbers(Optional.ofNullable(user.getAssignedAssets())
                .orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toList())); // Keep it as List<Asset>

        return dto;
    }

    // Convert UserDTO to User entity
    private User convertDtoToUser(UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setRole(dto.getRole());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setDepartment(dto.getDepartment());
        user.setNote(dto.getNote());
        user.setLocation(dto.getLocation());
        user.setSite(dto.getSite());
        return user;
    }
}
