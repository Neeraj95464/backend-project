package AssetManagement.AssetManagement.entity;

import AssetManagement.AssetManagement.enums.Department;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Where(clause = "is_deleted = false")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    private String username;

    private String password; // Ensure secure storage (e.g., hashed passwords)

    private String employeeId;

    private String role;

    private String phoneNumber;

    @Nonnull
    private String email;

    @Enumerated(EnumType.STRING)
    private Department department;

    private String note;

    private String createdBy;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;

    @OneToMany(mappedBy = "assignedUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Asset> assignedAssets;

    @Column(nullable = false)
    private boolean isDeleted = false; // ✅ Soft delete flag

}
