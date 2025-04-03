package AssetManagement.AssetManagement.repository;

import AssetManagement.AssetManagement.entity.Contract;
import AssetManagement.AssetManagement.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByStatus(ContractStatus status);
}

