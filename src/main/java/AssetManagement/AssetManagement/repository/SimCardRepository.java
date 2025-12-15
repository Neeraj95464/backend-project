// package AssetManagement.AssetManagement.repository;
package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.SimCard;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.SimStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface SimCardRepository extends JpaRepository<SimCard, Long>, JpaSpecificationExecutor<SimCard> {
//    Optional<SimCard> findBySimTag(String simTag);
    Optional<SimCard> findByPhoneNumber(String phoneNumber);
    List<SimCard> findByStatus(SimStatus status);
    List<SimCard> findByAssignedUser(User assignedUser);

    default Page<SimCard> filterSims(
            String phoneNumber,
            String provider,
            String status,
            String employeeId,
            Long departmentId,
            Long siteId,
            Long locationId,
            String search,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable
    ) {
        return findAll((root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

//            if (mobileNumber != null && !mobileNumber.isEmpty()) {
//                predicates.add(cb.like(root.get("mobileNumber"), "%" + mobileNumber + "%"));
//            }
//
//            if (simNumber != null && !simNumber.isEmpty()) {
//                predicates.add(cb.like(root.get("simNumber"), "%" + simNumber + "%"));
//            }

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                predicates.add(cb.equal(root.get("phoneNumber"), phoneNumber));
            }

            if (provider != null && !provider.isEmpty()) {
                predicates.add(cb.equal(root.get("provider"), provider));
            }

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (employeeId != null && !employeeId.isEmpty()) {
                predicates.add(cb.equal(root.get("assignedEmployeeId"), employeeId));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("departmentId"), departmentId));
            }

//            if (siteId != null) {
//                predicates.add(cb.equal(root.get("siteId"), siteId));
//            }
//
//            if (locationId != null) {
//                predicates.add(cb.equal(root.get("locationId"), locationId));
//            }

            // For siteId (foreign key)
            if (siteId != null) {
                predicates.add(cb.equal(root.get("site").get("id"), siteId));
            }

// For locationId (foreign key)
            if (locationId != null) {
                predicates.add(cb.equal(root.get("location").get("id"), locationId));
            }


            if (search != null && !search.isEmpty()) {
                predicates.add(cb.or(
                        cb.like(root.get("mobileNumber"), "%" + search + "%"),
                        cb.like(root.get("simNumber"), "%" + search + "%"),
                        cb.like(root.get("provider"), "%" + search + "%")
                ));
            }

            if (createdAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            }

            if (createdBefore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    boolean existsByPhoneNumber(String trim);
}
