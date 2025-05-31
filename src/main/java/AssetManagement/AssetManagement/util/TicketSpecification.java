package AssetManagement.AssetManagement.util;


import AssetManagement.AssetManagement.entity.Ticket;
import AssetManagement.AssetManagement.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TicketSpecification {

    // Restricted version
    public static Specification<Ticket> getFilteredTickets(Long ticketId, String title, User currentUser) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ticketId != null) {
                predicates.add(builder.equal(root.get("id"), ticketId));
            }

            if (title != null && !title.trim().isEmpty()) {
                predicates.add(
                        builder.like(
                                builder.lower(root.get("title")),
                                "%" + title.toLowerCase() + "%"
                        )
                );
            }

            Predicate isEmployee = builder.equal(root.get("employee").get("id"), currentUser.getId());
            Predicate isAssignee = builder.equal(root.get("assignee").get("id"), currentUser.getId());
            predicates.add(builder.or(isEmployee, isAssignee));

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ✅ Add this unrestricted version for ADMIN
    public static Specification<Ticket> getFilteredTickets(Long ticketId, String title) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ticketId != null) {
                predicates.add(builder.equal(root.get("id"), ticketId));
            }

            if (title != null && !title.trim().isEmpty()) {
                predicates.add(
                        builder.like(
                                builder.lower(root.get("title")),
                                "%" + title.toLowerCase() + "%"
                        )
                );
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

