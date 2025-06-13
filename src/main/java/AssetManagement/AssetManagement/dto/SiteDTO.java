package AssetManagement.AssetManagement.dto;

import lombok.Data;

import java.util.List;

// SiteRequestDTO.java
@Data
public class SiteDTO {
    private Long id;
    private String name;
    private String region;
    private String country;
    private List<LocationDTO> locations;
}

