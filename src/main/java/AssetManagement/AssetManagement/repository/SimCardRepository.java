//// package AssetManagement.AssetManagement.repository;
//package AssetManagement.AssetManagement.repository;
//
//import AssetManagement.AssetManagement.entity.SimCard;
//import AssetManagement.AssetManagement.entity.User;
//import AssetManagement.AssetManagement.enums.SimStatus;
//import jakarta.persistence.criteria.Predicate;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface SimCardRepository extends JpaRepository<SimCard, Long>, JpaSpecificationExecutor<SimCard> {
////    Optional<SimCard> findBySimTag(String simTag);
//    Optional<SimCard> findByPhoneNumber(String phoneNumber);
//    List<SimCard> findByStatus(SimStatus status);
//    List<SimCard> findByAssignedUser(User assignedUser);
//
////    default Page<SimCard> filterSims(
////            String phoneNumber,
////            String provider,
////            String status,
////            String employeeId,
////            Long departmentId,
////            Long siteId,
////            Long locationId,
////            String search,
////            LocalDateTime createdAfter,
////            LocalDateTime createdBefore,
////            Pageable pageable
////    ) {
////        return findAll((root, query, cb) -> {
////
////            List<Predicate> predicates = new ArrayList<>();
////
////            if (phoneNumber != null && !phoneNumber.isEmpty()) {
////                predicates.add(cb.equal(root.get("phoneNumber"), phoneNumber));
////            }
////
////            if (provider != null && !provider.isEmpty()) {
////                predicates.add(cb.equal(root.get("provider"), provider));
////            }
////
////            if (status != null && !status.isEmpty()) {
////                predicates.add(cb.equal(root.get("status"), status));
////            }
////
////            if (employeeId != null && !employeeId.isEmpty()) {
////                predicates.add(cb.equal(root.get("assignedEmployeeId"), employeeId));
////            }
////
////            if (departmentId != null) {
////                predicates.add(cb.equal(root.get("departmentId"), departmentId));
////            }
////
////            // For siteId (foreign key)
////            if (siteId != null) {
////                predicates.add(cb.equal(root.get("site").get("id"), siteId));
////            }
////
////            if (locationId != null) {
////                predicates.add(cb.equal(root.get("location").get("id"), locationId));
////            }
////
////
////            if (search != null && !search.isEmpty()) {
////                predicates.add(cb.or(
////                        cb.like(root.get("mobileNumber"), "%" + search + "%"),
////                        cb.like(root.get("simNumber"), "%" + search + "%"),
////                        cb.like(root.get("provider"), "%" + search + "%")
////                ));
////            }
////
////            if (createdAfter != null) {
////                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
////            }
////
////            if (createdBefore != null) {
////                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
////            }
////
////            return cb.and(predicates.toArray(new Predicate[0]));
////        }, pageable);
////    }
//
//    default Page<SimCard> filterSims(
//            String phoneNumber,
//            String provider,
//            String status,
//            String employeeId,
//            Long departmentId,
//            Long siteId,
//            Long locationId,
//            String search,
//            LocalDateTime createdAfter,
//            LocalDateTime createdBefore,
//            Pageable pageable
//    ) {
//        return findAll((root, query, cb) -> {
//
//            List<Predicate> predicates = new ArrayList<>();
//
//            if (phoneNumber != null && !phoneNumber.isEmpty()) {
//                predicates.add(cb.equal(root.get("phoneNumber"), phoneNumber));
//            }
//
//            if (provider != null && !provider.isEmpty()) {
//                predicates.add(cb.equal(root.get("provider"), provider));
//            }
//
//            if (status != null && !status.isEmpty()) {
//                // status is enum; if you pass string, you can compare as string name
//                predicates.add(cb.equal(root.get("status"), SimStatus.valueOf(status)));
//                // or if status already SimStatus in service, pass as SimStatus and use directly
//                // predicates.add(cb.equal(root.get("status"), statusEnum));
//            }
//
//            // ✅ filter by assigned user's employeeId
//            if (employeeId != null && !employeeId.isEmpty()) {
//                predicates.add(
//                        cb.equal(root.get("assignedUser").get("employeeId"), employeeId)
//                );
//            }
//
//            // ✅ departmentId lives on User or Location/Site (not on SimCard directly)
//            // If department is on User:
//            if (departmentId != null) {
//                predicates.add(
//                        cb.equal(root.get("assignedUser").get("department").get("id"), departmentId)
//                );
//            }
//            // Or if your Department is enum, adjust accordingly.
//
//            // ✅ Site
//            if (siteId != null) {
//                predicates.add(cb.equal(root.get("site").get("id"), siteId));
//            }
//
//            // ✅ Location
//            if (locationId != null) {
//                predicates.add(cb.equal(root.get("location").get("id"), locationId));
//            }
//
//            // ✅ search: your SimCard has phoneNumber, iccid, imsi, provider
//            if (search != null && !search.isEmpty()) {
//                String like = "%" + search + "%";
//                predicates.add(cb.or(
//                        cb.like(root.get("phoneNumber"), like),
//                        cb.like(root.get("iccid"), like),
//                        cb.like(root.get("imsi"), like),
//                        cb.like(root.get("note"),like),
//                        cb.like(root.get("provider").as(String.class), like)
//                ));
//            }
//
//            if (createdAfter != null) {
//                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
//            }
//
//            if (createdBefore != null) {
//                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
//            }
//
//            return cb.and(predicates.toArray(new Predicate[0]));
//        }, pageable);
//    }
//
//
//
//    boolean existsByPhoneNumber(String trim);
//}


package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.SimCard;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.SimStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface SimCardRepository extends JpaRepository<SimCard, Long>, JpaSpecificationExecutor<SimCard> {

    Optional<SimCard> findByPhoneNumber(String phoneNumber);

    List<SimCard> findByStatus(SimStatus status);

    List<SimCard> findByAssignedUser(User assignedUser);

    boolean existsByPhoneNumber(String trim);

    // ---- shared Specification builder ----
    private Specification<SimCard> buildFilterSpec(
            String phoneNumber,
            String provider,
            String status,
            String employeeId,
            Long departmentId,
            Long siteId,
            Long locationId,
            String search,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                predicates.add(cb.equal(root.get("phoneNumber"), phoneNumber));
            }

            if (provider != null && !provider.isEmpty()) {
                predicates.add(cb.equal(root.get("provider"), provider));
            }

            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), SimStatus.valueOf(status)));
            }

            if (employeeId != null && !employeeId.isEmpty()) {
                predicates.add(
                        cb.equal(root.get("assignedUser").get("employeeId"), employeeId)
                );
            }

            if (departmentId != null) {
                predicates.add(
                        cb.equal(root.get("assignedUser").get("department").get("id"), departmentId)
                );
            }

            if (siteId != null) {
                predicates.add(cb.equal(root.get("site").get("id"), siteId));
            }

            if (locationId != null) {
                predicates.add(cb.equal(root.get("location").get("id"), locationId));
            }

            if (search != null && !search.isEmpty()) {
                String like = "%" + search + "%";
                predicates.add(cb.or(
                        cb.like(root.get("phoneNumber"), like),
                        cb.like(root.get("iccid"), like),
                        cb.like(root.get("imsi"), like),
                        cb.like(root.get("note"), like),
                        cb.like(root.get("provider").as(String.class), like)
                ));
            }

            if (createdAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter));
            }

            if (createdBefore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ---- existing paged filter ----
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
        return findAll(
                buildFilterSpec(phoneNumber, provider, status, employeeId,
                        departmentId, siteId, locationId, search, createdAfter, createdBefore),
                pageable
        );
    }

    // ---- new non-paged filter for export ----
    default List<SimCard> filterSimsForExport(
            String phoneNumber,
            String provider,
            String status,
            String employeeId,
            Long departmentId,
            Long siteId,
            Long locationId,
            String search,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore
    ) {
        return findAll(
                buildFilterSpec(phoneNumber, provider, status, employeeId,
                        departmentId, siteId, locationId, search, createdAfter, createdBefore)
        ); // uses JpaSpecificationExecutor.findAll(Specification)[web:39][web:77]
    }
}
