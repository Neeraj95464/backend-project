package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);


    @Query("SELECT u FROM User u WHERE " +
            "(:id IS NULL OR u.id = :id) AND " +
            "(:username IS NULL OR u.username LIKE %:username%) AND " +
            "(:phoneNumber IS NULL OR u.phoneNumber LIKE %:phoneNumber%) AND " +
            "(:email IS NULL OR u.email LIKE %:email%)")
    Page<User> findUsers(
            @Param("id") Long id,
            @Param("username") String username,
            @Param("phoneNumber") String phoneNumber,
            @Param("email") String email,
            Pageable pageable);

}

