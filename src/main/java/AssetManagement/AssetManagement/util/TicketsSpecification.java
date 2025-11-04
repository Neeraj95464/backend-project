package AssetManagement.AssetManagement.util;

import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.TicketStatus;
import AssetManagement.AssetManagement.enums.TicketCategory;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;

import java.time.LocalDateTime;

public class TicketsSpecification {

    public static Specification<Ticket> hasTitle(String title) {
        return (root, query, criteriaBuilder) ->
                title == null ? null : criteriaBuilder.like(root.get("title"), "%" + title + "%");
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasCategory(TicketCategory category) {
        return (root, query, criteriaBuilder) ->
                category == null ? null : criteriaBuilder.equal(root.get("category"), category);
    }

    public static Specification<Ticket> hasEmployeeId(String employeeId) {
        return (root, query, criteriaBuilder) -> {
            if (employeeId == null) return null;
            Join<Object, Object> employeeJoin = root.join("employee");
            return criteriaBuilder.equal(employeeJoin.get("employeeId"), employeeId);
        };
    }

    public static Specification<Ticket> createdAfter(LocalDateTime date) {
        return (root, query, cb) -> date == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Ticket> createdBefore(LocalDateTime date) {
        return (root, query, cb) -> date == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Ticket> hasLocationId(Long locationId) {
        return (root, query, cb) -> {
            if (locationId == null) return null;
            Join<Ticket, Location> locationJoin = root.join("location");
            return cb.equal(locationJoin.get("id"), locationId);
        };
    }

    public static Specification<Ticket> hasAssigneeId(Long assigneeId) {
        return (root, query, cb) -> {
            if (assigneeId == null) return null;
            Join<Ticket, User> assigneeJoin = root.join("assignee");
            return cb.equal(assigneeJoin.get("id"), assigneeId);
        };
    }

    public static Specification<Ticket> hasAssigneeEmployeeId(String assigneeEmployeeId) {
        return (root, query, cb) -> {
            if (assigneeEmployeeId == null) return null;
            Join<Ticket, User> assigneeJoin = root.join("assignee");
            return cb.equal(assigneeJoin.get("employeeId"), assigneeEmployeeId);
        };
    }


    public static Specification<Ticket> globalSearch(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return null;
            }
            String likePattern = "%" + searchTerm.trim().toLowerCase() + "%";

            Join<Ticket, Location> locationJoin = root.join("location", JoinType.LEFT);
            Join<Ticket, User> assigneeJoin = root.join("assignee", JoinType.LEFT);
            Join<Ticket, User> employeeJoin = root.join("employee", JoinType.LEFT);

            Predicate ticketIdMatch = cb.like(cb.lower(root.get("id").as(String.class)), likePattern);
            Predicate titleMatch = cb.like(cb.lower(root.get("title")), likePattern);
            Predicate locationNameMatch = cb.like(cb.lower(locationJoin.get("name")), likePattern);
            Predicate assigneeNameMatch = cb.like(cb.lower(assigneeJoin.get("username")), likePattern);
            Predicate employeeNameMatch = cb.like(cb.lower(employeeJoin.get("username")), likePattern);

            return cb.or(ticketIdMatch, titleMatch, locationNameMatch, assigneeNameMatch, employeeNameMatch);
        };
    }


    public static Specification<Ticket> hasSiteAndLocation(Long siteId, Long locationId) {
        return (root, query, cb) -> {
            if (siteId == null) {
                return null; // No site filter, no condition
            }

            // Join Ticket -> Location
            Join<Ticket, Location> locationJoin = root.join("location", JoinType.INNER);

            // Join Location -> Site
            Join<Location, Site> siteJoin = locationJoin.join("site", JoinType.INNER);

            // Predicate for site
            var sitePredicate = cb.equal(siteJoin.get("id"), siteId);

            if (locationId != null) {
                // Both site and location filter applied
                var locationPredicate = cb.equal(locationJoin.get("id"), locationId);
                return cb.and(sitePredicate, locationPredicate);
            } else {
                // Only site filter applied (all locations in the site)
                return sitePredicate;
            }
        };
    }


}

