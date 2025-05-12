package AssetManagement.AssetManagement.util;

import AssetManagement.AssetManagement.entity.Ticket;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class TicketSpecification {

    public static Specification<Ticket> getFilteredTickets(Long ticketId, String title) {
        return (root, query, builder) -> {
            Predicate predicate = builder.conjunction(); // Start with an empty predicate

            if (ticketId != null) {
                predicate = builder.and(predicate, builder.equal(root.get("id"), ticketId)); // Search by ticketId
            }

            if (title != null && !title.trim().isEmpty()) {
                predicate = builder.and(predicate, builder.like(builder.lower(root.get("title")), "%" + title.toLowerCase() + "%")); // Search by title
            }

            return predicate;
        };
    }
}
