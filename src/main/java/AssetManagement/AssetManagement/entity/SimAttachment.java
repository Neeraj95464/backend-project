package AssetManagement.AssetManagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sim_id")
    private SimCard simCard;

    private String fileName;
    private String fileType;
    private Long fileSize;        // optional: KB/MB
    private String fileUrl;       // path or cloud URL

    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(length = 500)
    private String note; // optional
}
