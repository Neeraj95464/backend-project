package AssetManagement.AssetManagement.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class ChildAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String warranty;
    private String purchaseFrom;

    @ManyToOne
    @JoinColumn(name = "parent_asset_id")
    private Asset parentAsset;
}

