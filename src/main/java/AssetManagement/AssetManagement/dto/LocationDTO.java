package AssetManagement.AssetManagement.dto;

import lombok.Data;

// LocationRequestDTO.java
@Data
public class LocationDTO {
    private Long id;
    private String name;
    private String address;
    private String postalCode;
    private String state;
    private Long siteId;
}

