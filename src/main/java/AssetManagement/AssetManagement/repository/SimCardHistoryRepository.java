// package AssetManagement.AssetManagement.repository;
package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.SimCardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimCardHistoryRepository extends JpaRepository<SimCardHistory, Long> {
    List<SimCardHistory> findBySimCardIdOrderByPerformedAtDesc(Long simCardId);
}
