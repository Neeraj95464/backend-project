package AssetManagement.AssetManagement.dto;

import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.enums.Department;
import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Site;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Long id; // User ID (for updates and identification)

    private String username; // Username

    private String password; // Password (ensure it's hashed during storage)

    private String role; // Role (e.g., Admin, User)

    private String email; // Email address

    private String phoneNumber; // Phone number

    private Department department; // Department (ENUM)

    private String note; // Additional notes

    private Location location; // Location (entity association)

    private Site site; // Site (entity association)

    private List<Asset> serialNumbers; // List of assigned asset serial numbers

}
