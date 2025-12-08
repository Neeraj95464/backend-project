// package AssetManagement.AssetManagement.entity;
package AssetManagement.AssetManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Entity
@Table(name = "sim_card_history")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimCardHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to SimCard
    @ManyToOne
    @JoinColumn(name = "sim_card_id", nullable = false)
    private SimCard simCard;

    // Short description of event: ASSIGNED, UNASSIGNED, STATUS_CHANGE, INFO_UPDATE, etc.
    @Column(nullable = false)
    private String eventType;

    // Description/details (old->new for critical fields)
    @Column(length = 1000)
    private String details;

    // Optional reference to user who triggered the change (username or id)
    private String performedBy;

    private LocalDateTime performedAt;

}
