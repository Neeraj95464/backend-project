package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public List<Location> findByName(String name) {
        return locationRepository.findByNameContainingIgnoreCase(name);
    }

    // Retrieve locations by site ID
    public List<Location> getLocationsBySiteId(Long siteId) {
        return locationRepository.findBySiteId(siteId);
    }

    // Create a new location
    @Transactional
    public Location createLocation(Location location) {
        return locationRepository.save(location);
    }
}

