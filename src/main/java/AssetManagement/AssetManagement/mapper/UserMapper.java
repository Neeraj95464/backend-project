//package AssetManagement.AssetManagement.mapper;
//
//import AssetManagement.AssetManagement.dto.UserResponseDto;
//import AssetManagement.AssetManagement.entity.User;
//
//public class UserMapper {
//
//    public static UserResponseDto toDto(User user) {
//        UserResponseDto dto = new UserResponseDto();
//        dto.setId(user.getId());
//        dto.setUsername(user.getUsername());
//        dto.setEmployeeId(user.getEmployeeId());
//        dto.setRole(user.getRole());
//        dto.setPhoneNumber(user.getPhoneNumber());
//        dto.setEmail(user.getEmail());
//        dto.setDesignation(user.getDesignation());
//        dto.setDepartment(user.getDepartment());
//        dto.setSiteName(user.getSite() != null ? user.getSite().getName() : null);
//        dto.setLocationName(user.getLocation() != null ? user.getLocation().getName() : null);
//        return dto;
//    }
//}
//


package AssetManagement.AssetManagement.mapper;

import AssetManagement.AssetManagement.dto.UserResponseDto;
import AssetManagement.AssetManagement.entity.User;
import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    public static UserResponseDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setRole(user.getRole());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setEmail(user.getEmail());
        dto.setPersonalEmail(user.getPersonalEmail());
        dto.setDesignation(user.getDesignation());
        dto.setDepartment(user.getDepartment());
        dto.setSiteName(user.getSite() != null ? user.getSite().getName() : null);
        dto.setLocationName(user.getLocation() != null ? user.getLocation().getName() : null);
        dto.setAadharNumber(user.getAadharNumber());
        dto.setPanNumber(user.getPanNumber());
        dto.setNote(user.getNote());
        dto.setCreatedBy(user.getCreatedBy());
        dto.setLastUpdatedBy(user.getUpdatedBy());

        return dto;
    }

    public static List<UserResponseDto> toDtoList(List<User> users) {
        if (users == null) {
            return List.of();
        }
        return users.stream()
                .map(UserMapper::toDto)
                .collect(Collectors.toList());
    }
}
