package AssetManagement.AssetManagement.dto;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SimAttachmentDto {
    private Long id;
    private Long simId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileUrl;
    private LocalDateTime uploadedAt;
    private String note;
}
