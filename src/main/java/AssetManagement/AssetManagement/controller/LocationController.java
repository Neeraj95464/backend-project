package AssetManagement.AssetManagement.controller;

import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    @Autowired
    private LocationService locationService;

    @GetMapping("/site/{siteId}")
    public ResponseEntity<List<Location>> getLocationsBySite(@PathVariable Long siteId) {
        List<Location> locations = locationService.getLocationsBySiteId(siteId);
        if (locations.isEmpty()) {
            return ResponseEntity.noContent().build(); // Return 204 if no locations found
        }
        return ResponseEntity.ok(locations); // Return the list of locations
    }
    @PostMapping
    public ResponseEntity<?> createLocation(@RequestBody Location location) {
        try {
            // Call the service to save the location
            locationService.createLocation(location);
            // Return success response with status 201 (Created)
            return ResponseEntity.status(HttpStatus.CREATED).body("Location created successfully");
        } catch (Exception e) {
            // Handle potential errors and return a meaningful error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error occurred while creating the location: " + e.getMessage());
        }
    }
}
