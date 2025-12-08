package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.SimAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimAttachmentRepository extends JpaRepository<SimAttachment, Long> {
    List<SimAttachment> findBySimCardId(Long simId);
}
