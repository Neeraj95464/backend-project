package AssetManagement.AssetManagement.entity;

import AssetManagement.AssetManagement.enums.TicketDepartment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LocationAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User itExecutive;  // ✅ IT Executive

    @ManyToOne
    @JoinColumn(name="manager_id",nullable = false)
    private User locationManager;

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = false)
    private Location location; // ✅ Assigned Location

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketDepartment ticketDepartment;
}

