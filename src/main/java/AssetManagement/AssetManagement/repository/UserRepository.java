package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.Department;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
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

    // shared spec
    private Specification<User> buildFilterSpec(
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
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (employeeId != null && !employeeId.isEmpty()) {
                predicates.add(cb.equal(root.get("employeeId"), employeeId));
            }

            if (username != null && !username.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
            }

            if (role != null && !role.isEmpty()) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (department != null) {
                predicates.add(cb.equal(root.get("department"), department));
            }

            if (siteId != null) {
                predicates.add(cb.equal(root.get("site").get("id"), siteId));
            }

            if (locationId != null) {
                predicates.add(cb.equal(root.get("location").get("id"), locationId));
            }

            if (search != null && !search.isEmpty()) {
                String like = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("employeeId")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("phoneNumber")), like),
                        cb.like(cb.lower(root.get("designation")), like)
                ));
            }

            // if you have createdAt in User, uncomment and map field name
            // if (createdAfter != null) {
            //     predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            // }
            // if (createdBefore != null) {
            //     predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
            // }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // paginated filter
    default Page<User> filterUsers(
            String employeeId,
            String username,
            String role,
            Department department,
            Long siteId,
            Long locationId,
            String search,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable
    ) {
        return findAll(
                buildFilterSpec(employeeId, username, role, department,
                        siteId, locationId, search, createdAfter, createdBefore),
                pageable
        );
    }

    // non‑paged export
    default List<User> filterUsersForExport(
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
        return findAll(
                buildFilterSpec(employeeId, username, role, department,
                        siteId, locationId, search, createdAfter, createdBefore)
        ); // uses JpaSpecificationExecutor.findAll(Specification)[web:39]
    }
}

