package AssetManagement.AssetManagement.entity;

import AssetManagement.AssetManagement.enums.AssetType;
import AssetManagement.AssetManagement.enums.AssetStatus;
import AssetManagement.AssetManagement.enums.Department;
import AssetManagement.AssetManagement.util.AssetIdGenerator;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String assetTag;

    @Nonnull
    private String name;

    private String description;

    @Column(unique = true, nullable = false)
    private String serialNumber;

    private LocalDate purchaseDate;
    private String purchaseFrom;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AssetStatus status; // Enum for asset status

    private String brand;

    private String model;

    @Enumerated(EnumType.STRING)
    private AssetType assetType; // Enum for asset type

    @Enumerated(EnumType.STRING)
    private Department department; // Enum for department

    private String createdBy;

    private BigDecimal cost; // Changed from String to BigDecimal for monetary values

    @ManyToOne
    @JoinColumn(name = "location_id", referencedColumnName = "id")
    private Location location; // Relationship with Location entity

    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site; // Relationship with Site entity

    @ManyToOne
    @JoinColumn(name = "assigned_user_id", referencedColumnName = "id")
    @JsonBackReference
    private User assignedUser;

    // Fields for reservation
    private LocalDate reservationStartDate;
    private LocalDate reservationEndDate;

    private String statusNote;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssetPhoto> photos = new ArrayList<>();

    // Self-referencing relationship for Parent-Child Assets
    @OneToMany(mappedBy = "parentAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Asset> childAssets = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "parent_asset_id")
    private Asset parentAsset;

    @PrePersist
    public void generateAssetTag() {
        if (this.assetTag == null) {
            this.assetTag = "MGIT" + AssetIdGenerator.getNextId();
        }
    }
}
