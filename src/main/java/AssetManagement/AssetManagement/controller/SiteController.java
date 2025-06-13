package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.dto.LocationDTO;
import AssetManagement.AssetManagement.dto.SiteDTO;
import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.service.SiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sites")
public class SiteController {
    private final SiteService siteService;

    @Autowired
    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @PostMapping
    public ResponseEntity<Site> createSiteWithLocations(@RequestBody Site site) {
        Site savedSite = siteService.saveSiteWithLocations(site);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSite);
    }

    @GetMapping
    public ResponseEntity<List<Site>> getSites() {
        List<Site> sites = siteService.getSites();
        if (sites == null || sites.isEmpty()) {
            return ResponseEntity.noContent().build(); // Return 204 No Content if the list is empty
        }
        return ResponseEntity.ok(sites); // Return the list with 200 OK status
    }


    @GetMapping("/all")
    public ResponseEntity<List<SiteDTO>> getSitesWithLocation() {
        List<SiteDTO> sites = siteService.getAllSiteDTOs();
        if (sites.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(sites);
    }
    @PostMapping("/location")
    public ResponseEntity<LocationDTO> createLocationUnderSite(@RequestBody LocationDTO locationDTO) {
        System.out.println(locationDTO);
        LocationDTO location = siteService.addLocation(locationDTO);

        return ResponseEntity.ok(location);
    }


}

