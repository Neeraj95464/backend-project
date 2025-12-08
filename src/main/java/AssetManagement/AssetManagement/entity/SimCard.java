// package AssetManagement.AssetManagement.entity;
package AssetManagement.AssetManagement.entity;

import AssetManagement.AssetManagement.enums.SimProvider;
import AssetManagement.AssetManagement.enums.SimStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Where(clause = "status <> 'DELETED'") // keeps parity with your Asset pattern; adjust if needed
public class SimCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String phoneNumber; // msisdn

    @Column(unique = true)
    private String iccid;

    @Column(unique = true)
    private String imsi;

    @Enumerated(EnumType.STRING)
    private SimProvider provider;

    @Enumerated(EnumType.STRING)
    private SimStatus status = SimStatus.AVAILABLE;

    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    private LocalDateTime assignedAt;

    private LocalDate activatedAt;
    private LocalDate purchaseDate;
    private String purchaseFrom;
    private BigDecimal cost;

    private String createdBy;
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean assignmentUploaded = false;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;

    @OneToMany(mappedBy = "simCard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SimAttachment> attachments = new ArrayList<>();

    @Column(length = 1000)
    private String note;

    @OneToMany(mappedBy = "simCard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SimCardHistory> history = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
