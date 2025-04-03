package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.AssetDTO;
import AssetManagement.AssetManagement.dto.UserAssetDTO;
import AssetManagement.AssetManagement.dto.UserDTO;
import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.exception.UserNotFoundException;
import AssetManagement.AssetManagement.repository.AssetRepository;
import AssetManagement.AssetManagement.repository.UserRepository;
import AssetManagement.AssetManagement.util.AuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserAssetService {

    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private AssetService assetService;

    @Autowired
    private UserRepository userRepository;

    public List<AssetDTO> getAssetsForUser(String employeeId) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + employeeId + " not found")); // ✅ Custom exception

        List<Asset> assets = assetRepository.findByAssignedUser(user); // ✅ Fetch assets for user

        return assets.stream()
                .map(this::convertUserAssetToDTO)
                .collect(Collectors.toList()); // ✅ Convert to DTO
    }

    public UserDTO getCurrentUser() {
        String employeeId = AuthUtils.getAuthenticatedUsername();

        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userService.convertUserToDto(user); // ✅ Directly return the converted UserDTO
    }

//    public List<AssetDTO> getCurrentUserAssets() {
//        String employeeId = AuthUtils.getAuthenticatedUsername(); // Fetch authenticated user
//
//        User user = userRepository.findByEmployeeId(employeeId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        List<Asset> assetList = user.getAssignedAssets(); // Fetch assigned assets
//
//        // Convert each asset one by one using convertAssetToDto()
//        List<AssetDTO> assetDTOList = assetList.stream()
//                .map(assetService::convertAssetToDto) // ✅ Pass each asset one by one
//                .collect(Collectors.toList());
//
//        return assetDTOList;
//    }

    private AssetDTO convertUserAssetToDTO(Asset asset) {
        AssetDTO dto = new AssetDTO();
        dto.setAssetTag(asset.getAssetTag());
        dto.setName(asset.getName());

        dto.setDescription(asset.getDescription());
        dto.setSerialNumber(asset.getSerialNumber());
        dto.setPurchaseDate(asset.getPurchaseDate());

        // ✅ Handle null safety for Enums
        dto.setStatus(asset.getStatus() != null ? asset.getStatus().name() : "UNKNOWN");

        // ✅ Safely handle potential null values to prevent NullPointerExceptions
        dto.setLocationName(asset.getLocation() != null ? asset.getLocation().getName() : null);

        dto.setSiteName(asset.getSite() != null ? asset.getSite().getName() : null);

        dto.setAssignedUserName(asset.getAssignedUser() != null ? asset.getAssignedUser().getUsername() : null);

        dto.setReservationStartDate(asset.getReservationStartDate());
        dto.setReservationEndDate(asset.getReservationEndDate());
        dto.setStatusNote(asset.getStatusNote());
        dto.setBrand(asset.getBrand());
        dto.setModel(asset.getModel());

        // ✅ Ensure null safety for enums
        dto.setDepartment(asset.getDepartment() != null ? asset.getDepartment().toString() : null);
        dto.setAssetType(asset.getAssetType() != null ? asset.getAssetType().toString() : null);
        dto.setCreatedAt(asset.getCreatedAt());
        dto.setCreatedBy(asset.getCreatedBy());

        return dto;
    }
}

