package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.PaginatedResponse;
import AssetManagement.AssetManagement.dto.UserDTO;
import AssetManagement.AssetManagement.dto.UserResponseDto;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.Department;
import AssetManagement.AssetManagement.exception.UserNotFoundException;
import AssetManagement.AssetManagement.mapper.UserMapper;
import AssetManagement.AssetManagement.repository.UserRepository;
import AssetManagement.AssetManagement.util.AuthUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        String defaultUserPassword="test";
        // Encode and set the password
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        user.setPassword(passwordEncoder.encode(defaultUserPassword));

        // Assign default role
        user.setRole("USER");

        if(user.getEmail()==null && user.getPersonalEmail()== null){
            throw new RuntimeException("Both email value can't be null ");
        }
        if(user.getEmail() == null){
            user.setEmail(user.getPersonalEmail());
            user.setNote(user.getNote() + "Personal email is saved in professional email " +
                    "pls update the professional one if there allocated ");
        }

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

        String updater = AuthUtils.getAuthenticatedUserExactName();

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        // Update user fields
        existingUser.setUsername(userDto.getUsername());
        existingUser.setRole(userDto.getRole());
        existingUser.setEmail(userDto.getEmail());
        existingUser.setPanNumber(userDto.getPanNumber());
        existingUser.setDesignation(userDto.getDesignation());
        existingUser.setAadharNumber(userDto.getAadharNumber());
        existingUser.setPersonalEmail(userDto.getPersonalEmail());
        existingUser.setPhoneNumber(String.valueOf(userDto.getPhoneNumber()));
        existingUser.setDepartment(userDto.getDepartment());
        existingUser.setNote(userDto.getNote());
        existingUser.setLocation(userDto.getLocation());
        existingUser.setSite(userDto.getSite());
        existingUser.setUpdatedBy(updater);

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
        dto.setDesignation(user.getDesignation());
        dto.setAadharNumber(user.getAadharNumber());
        dto.setPanNumber(user.getPanNumber());
        dto.setPersonalEmail(user.getPersonalEmail());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setDepartment(user.getDepartment());
        dto.setNote(user.getNote());
        dto.setLocation(user.getLocation());
        dto.setSite(user.getSite());
        dto.setSerialNumbers(null);

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


        public void resetPasswordByEmployeeId(String employeeId) {
            // 1) check current user is ADMIN
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getAuthorities().stream().noneMatch(
                    a -> a.getAuthority().equals("ROLE_ADMIN")
            )) {
                throw new SecurityException("Only ADMIN can reset passwords");
            }

            // 2) find target user by employeeId
            User user = userRepository.findByEmployeeId(employeeId)
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + employeeId));

            // 3) set static password "test" (encoded)
            String encoded = passwordEncoder.encode("test");
            user.setPassword(encoded);

            System.out.println("Saving new password "+user.getEmployeeId()+" "+user.getPassword());

            // 4) save
            userRepository.save(user);
        }

    public PaginatedResponse<UserResponseDto> filterUsers(
            String employeeId,
            String username,
            String role,
            Department department,
            Long siteId,
            Long locationId,
            String search,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<User> data = userRepository.filterUsers(
                employeeId,
                username,
                role,
                department,
                siteId,
                locationId,
                search,
                createdAfter,
                createdBefore,
                pageable
        );

        List<UserResponseDto> content = data.getContent()
                .stream()
                .map(UserMapper::toDto)
                .toList();

        return new PaginatedResponse<>(
                content,
                data.getNumber(),
                data.getSize(),
                data.getTotalElements(),
                data.getTotalPages(),
                data.isLast()
        );
    }

    public List<UserResponseDto> filterUsersForExport(
            String employeeId,
            String username,
            String role,
            Department department,
            Long siteId,
            Long locationId,
            String search,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        List<User> data = userRepository.filterUsersForExport(
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

        return data.stream()
                .map(UserMapper::toDto)
                .toList();
    }

}
