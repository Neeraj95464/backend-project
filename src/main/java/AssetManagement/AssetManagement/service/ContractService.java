package AssetManagement.AssetManagement.service;


import AssetManagement.AssetManagement.entity.Contract;
import AssetManagement.AssetManagement.exception.ResourceNotFoundException;
import AssetManagement.AssetManagement.repository.ContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContractService {

    private final ContractRepository contractRepository;

    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Contract getContractById(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found with ID: " + id));
    }

    public Contract createContract(Contract contract) {
        return contractRepository.save(contract);
    }

    public Contract updateContract(Long id, Contract contractDetails) {
        Contract existingContract = getContractById(id);

        existingContract.setContractName(contractDetails.getContractName());
        existingContract.setCustomerName(contractDetails.getCustomerName());
        existingContract.setCustomerEmail(contractDetails.getCustomerEmail());
        existingContract.setStartDate(contractDetails.getStartDate());
        existingContract.setEndDate(contractDetails.getEndDate());
        existingContract.setStatus(contractDetails.getStatus());
        existingContract.setContractType(contractDetails.getContractType());
        existingContract.setPriority(contractDetails.getPriority());
        existingContract.setSlaResponseTime(contractDetails.getSlaResponseTime());
        existingContract.setSlaResolutionTime(contractDetails.getSlaResolutionTime());
        existingContract.setAssignedTo(contractDetails.getAssignedTo());
        existingContract.setDescription(contractDetails.getDescription());

        return contractRepository.save(existingContract);
    }

    public void deleteContract(Long id) {
        Contract contract = getContractById(id);
        contractRepository.delete(contract);
    }
}

