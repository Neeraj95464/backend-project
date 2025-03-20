package AssetManagement.AssetManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_history")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;  // ✅ Relationship with Asset

    @Column(nullable = false)
    private String changedAttribute;  // ✅ What field changed

    @Column(nullable = false, length = 500)
    private String oldValue;  // ✅ Old value

    @Column(nullable = false, length = 500)
    private String newValue;  // ✅ New value

    @Column(nullable = false)
    private String modifiedBy;  // ✅ Who made the change

    @Column(nullable = false)
    private LocalDateTime modifiedAt;  // ✅ When was it changed?

}
