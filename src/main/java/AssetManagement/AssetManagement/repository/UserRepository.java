package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmployeeId(String empId);
    Optional<User> findByIdAndIsDeletedFalse(Long id);

    Optional<User> findByEmail(String email);
    List<User> findByIsDeletedFalse();

    @Query("SELECT u FROM User u WHERE " +
            "(:employeeId IS NULL OR u.employeeId = :employeeId) AND " +
            "(:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))) AND " +
            "(:phoneNumber IS NULL OR u.phoneNumber = :phoneNumber) AND " +
            "(:email IS NULL OR LOWER(u.email) = LOWER(:email))")
    Page<User> findUsers(
            @Param("employeeId") String employeeId,
            @Param("username") String username,
            @Param("phoneNumber") String phoneNumber,
            @Param("email") String email,
            Pageable pageable
    );

    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmail(String email);

    List<User> findAllByEmail(String senderEmail);

    @Query("SELECT u.employeeId FROM User u WHERE u.employeeId LIKE 'temp%'")
    List<String> findAllEmployeeIdsStartingWith(@Param("prefix") String prefix);

}

