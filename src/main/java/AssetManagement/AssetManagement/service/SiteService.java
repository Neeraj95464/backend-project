package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.LocationDTO;
import AssetManagement.AssetManagement.dto.SiteDTO;
import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.repository.LocationRepository;
import AssetManagement.AssetManagement.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final LocationRepository locationRepository;

    public SiteService(SiteRepository siteRepository, LocationRepository locationRepository) {
        this.siteRepository = siteRepository;
        this.locationRepository = locationRepository;
    }

    // Save a site along with its associated locations
    @Transactional
    public Site saveSiteWithLocations(Site site) {
        return siteRepository.save(site);
    }

    // Retrieve all sites
    public List<Site> getSites() {
        return siteRepository.findAll();
    }

    public List<SiteDTO> getAllSiteDTOs() {
        List<Site> sites = siteRepository.findAll(); // You can use @EntityGraph or JOIN FETCH here if needed
        return sites.stream()
                .map(this::toSiteDTO)
                .collect(Collectors.toList());
    }


//    public SiteDTO toSiteDTO(Site site) {
//        SiteDTO dto = new SiteDTO();
//        dto.setId(site.getId());
//        dto.setName(site.getName());
//        dto.setRegion(site.getRegion());
//        dto.setCountry(site.getCountry());
//
//        List<LocationDTO> locations = site.getLocations().stream().map(loc -> {
//            LocationDTO lDto = new LocationDTO();
//            lDto.setSiteId(loc.getId());
//            lDto.setId(loc.getId());
//            lDto.setName(loc.getName());
//            lDto.setAddress(loc.getAddress());
//            return lDto;
//        }).collect(Collectors.toList());
//
//        dto.setLocations(locations);
//        return dto;
//    }

    public SiteDTO toSiteDTO(Site site) {
        SiteDTO dto = new SiteDTO();
        dto.setId(site.getId());
        dto.setName(site.getName());
        dto.setRegion(site.getRegion());
        dto.setCountry(site.getCountry());

        List<LocationDTO> locations = site.getLocations().stream().map(loc -> {
            LocationDTO lDto = new LocationDTO();
            lDto.setId(loc.getId());           // ✅ Location's own ID
            lDto.setSiteId(site.getId());      // ✅ Parent Site's ID
            lDto.setName(loc.getName());
            lDto.setAddress(loc.getAddress());
            lDto.setPostalCode(loc.getPostalCode());  // ✅ Add these too
            lDto.setState(loc.getState());
            return lDto;
        }).collect(Collectors.toList());

        dto.setLocations(locations);
        return dto;
    }



    public LocationDTO addLocation(LocationDTO dto) {
        Site site = siteRepository.findById(dto.getSiteId())
                .orElseThrow(() -> new RuntimeException("Site not found with ID: " + dto.getSiteId()));

        Location location = new Location();
        location.setName(dto.getName());
        location.setAddress(dto.getAddress());
        location.setPostalCode(dto.getPostalCode());
        location.setState(dto.getState());
        location.setSite(site);

        Location saved = locationRepository.save(location);

        // Convert to DTO
        LocationDTO response = new LocationDTO();
        response.setName(saved.getName());
        response.setAddress(saved.getAddress());
        response.setPostalCode(saved.getPostalCode());
        response.setState(saved.getState());
        response.setSiteId(site.getId());

        return response;
    }

}
