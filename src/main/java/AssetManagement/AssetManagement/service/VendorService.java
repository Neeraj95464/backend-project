package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.entity.Vendor;
import AssetManagement.AssetManagement.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class VendorService {

    @Autowired
    private VendorRepository vendorRepository;

    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    public Optional<Vendor> getVendorById(Long id) {
        return vendorRepository.findById(id);
    }

    public Vendor createVendor(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    public Vendor updateVendor(Long id, Vendor vendorDetails) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        vendor.setName(vendorDetails.getName());
        vendor.setEmail(vendorDetails.getEmail());
        vendor.setPhone(vendorDetails.getPhone());
        vendor.setCompany(vendorDetails.getCompany());
        vendor.setAddress(vendorDetails.getAddress());
        vendor.setWebsite(vendorDetails.getWebsite());
        vendor.setContactPerson(vendorDetails.getContactPerson());
        vendor.setGstNumber(vendorDetails.getGstNumber());
        vendor.setIndustryType(vendorDetails.getIndustryType());
        vendor.setDescription(vendorDetails.getDescription());

        return vendorRepository.save(vendor);
    }

    public void deleteVendor(Long id) {
        vendorRepository.deleteById(id);
    }
}

