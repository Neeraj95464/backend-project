package AssetManagement.AssetManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimBillingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sim_card_id", nullable = false)
    private SimCard simCard;

    private LocalDate billingMonth;   // 2025-12-01 (month identifier)

    private BigDecimal monthlyCost;   // e.g., ₹199 plan
    private BigDecimal extraCharges;  // e.g., roaming, penalty
    private BigDecimal total;         // monthlyCost + extraCharges

    private String description;       // e.g., “December bill with add-on”
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}

