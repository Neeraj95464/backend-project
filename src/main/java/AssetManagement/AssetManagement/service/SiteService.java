package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SiteService {

    private final SiteRepository siteRepository;

    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
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
}
