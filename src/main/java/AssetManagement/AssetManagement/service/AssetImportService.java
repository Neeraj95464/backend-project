package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.AssetImportResult;
import AssetManagement.AssetManagement.dto.RowError;
import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.enums.AssetStatus;
import AssetManagement.AssetManagement.enums.AssetType;
import AssetManagement.AssetManagement.enums.Department;
import AssetManagement.AssetManagement.repository.AssetRepository;
import AssetManagement.AssetManagement.repository.LocationRepository;
import AssetManagement.AssetManagement.repository.SiteRepository;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssetImportService {

    private final AssetRepository assetRepository;
    private final AssetService assetService;
    private final SiteRepository siteRepository;
    private final LocationRepository locationRepository;

    public AssetImportService(AssetRepository assetRepository, AssetService assetService, SiteRepository siteRepository, LocationRepository locationRepository) {
        this.assetRepository = assetRepository;
        this.assetService = assetService;
        this.siteRepository = siteRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public AssetImportResult importAssetsFromExcel(MultipartFile file) {
        int success = 0;
        List<RowError> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0); // first sheet
            // assume first row is header
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }

                try {
                    Asset asset = mapRowToAsset(row);
                    // reuse your existing logic – this calls validateUniqueFields inside
                    assetService.saveAsset(asset);
                    success++;
                } catch (Exception ex) {
                    errors.add(new RowError(rowIdx + 1, ex.getMessage())); // +1 for human-friendly row number
                }
            }
        } catch (IOException e) {
            errors.add(new RowError(0, "Failed to read Excel file: " + e.getMessage()));
        }

        int failure = errors.size();
        return new AssetImportResult(success, failure, errors);
    }

    // Map Excel row -> Asset entity
//    private Asset mapRowToAsset(Row row) {
//        Asset asset = new Asset();
//
//        // Example: adjust indices to your Excel column order
//        // Use helper to read string/numeric safely
//        asset.setName(getStringCell(row, 0));
//        asset.setDescription(getStringCell(row, 1));
//        asset.setSerialNumber(getStringCell(row, 2));
//        asset.setAssetTag(getStringCell(row, 3));
//        asset.setBrand(getStringCell(row, 4));
//        asset.setModel(getStringCell(row, 5));
//
//        // Example for enums (must match exact enum names)
//        String assetTypeStr = getStringCell(row, 6);
//        if (assetTypeStr != null && !assetTypeStr.isBlank()) {
//            asset.setAssetType(AssetType.valueOf(assetTypeStr.trim()));
//        }
//
//        String deptStr = getStringCell(row, 7);
//        if (deptStr != null && !deptStr.isBlank()) {
//            asset.setDepartment(Department.valueOf(deptStr.trim()));
//        }
//
//        // cost as numeric
//        Cell costCell = row.getCell(8);
//        if (costCell != null && costCell.getCellType() == CellType.NUMERIC) {
//            asset.setCost(BigDecimal.valueOf(costCell.getNumericCellValue()));
//        }
//
//        // you can also set default status, site, location, etc. here if needed
//
//        return asset;
//    }

//    private Asset mapRowToAsset(Row row) {
//        Asset asset = new Asset();
//
//        asset.setName(getStringCell(row, 0));
//        asset.setDescription(getStringCell(row, 1));
//        asset.setSerialNumber(getStringCell(row, 2));
//        asset.setAssetTag(null); // let @PrePersist generate
//
//        asset.setPurchaseDate(getDateCell(row, 4));
//        asset.setPurchaseFrom(getStringCell(row, 5));
//        asset.setBrand(getStringCell(row, 6));
//        asset.setModel(getStringCell(row, 7));
//
//        String assetTypeStr = getStringCell(row, 8);
//        if (assetTypeStr != null && !assetTypeStr.isBlank()) {
//            asset.setAssetType(AssetType.valueOf(assetTypeStr.trim()));
//        }
//
//        String deptStr = getStringCell(row, 9);
//        if (deptStr != null && !deptStr.isBlank()) {
//            asset.setDepartment(Department.valueOf(deptStr.trim()));
//        }
//
//        Cell costCell = row.getCell(10);
//        if (costCell != null && costCell.getCellType() == CellType.NUMERIC) {
//            asset.setCost(BigDecimal.valueOf(costCell.getNumericCellValue()));
//        }
//
//        String statusStr = getStringCell(row, 11);
//        if (statusStr != null && !statusStr.isBlank()) {
//            asset.setStatus(AssetStatus.valueOf(statusStr.trim()));
//        }
//
//        asset.setStatusNote(getStringCell(row, 12));
//
//        // --- Site & Location by name ---
//        String siteName = getStringCell(row, 13);
//        String locationName = getStringCell(row, 14);
//
//        if (siteName != null && !siteName.isBlank()) {
//            Site site = siteRepository.findByName(siteName.trim())
//                    .orElseThrow(() -> new IllegalArgumentException(
//                            "Site not found with name: " + siteName));
//            asset.setSite(site);
//
//            if (locationName != null && !locationName.isBlank()) {
//                Location location = locationRepository
//                        .findByNameAndSite(locationName.trim(), site)
//                        .orElseThrow(() -> new IllegalArgumentException(
//                                "Location '" + locationName + "' not found in site '" + siteName + "'"));
//                asset.setLocation(location);
//            }
//        }
//
//        return asset;
//    }

    private Asset mapRowToAsset(Row row) {
        Asset asset = new Asset();

        asset.setName(getStringCell(row, 0));
        asset.setDescription(getStringCell(row, 1));
        asset.setSerialNumber(getStringCell(row, 2));
        asset.setAssetTag(null); // let @PrePersist generate

        asset.setPurchaseDate(getDateCell(row, 3));      // ✅ 3 = purchaseDate
        asset.setPurchaseFrom(getStringCell(row, 4));    // ✅ 4 = purchaseFrom
        asset.setBrand(getStringCell(row, 5));           // ✅ 5 = brand
        asset.setModel(getStringCell(row, 6));           // ✅ 6 = model

        String assetTypeStr = getStringCell(row, 7);     // ✅ 7 = assetType
        if (assetTypeStr != null && !assetTypeStr.isBlank()) {
            asset.setAssetType(AssetType.valueOf(assetTypeStr.trim()));
        }

        String deptStr = getStringCell(row, 8);          // ✅ 8 = department
        if (deptStr != null && !deptStr.isBlank()) {
            asset.setDepartment(Department.valueOf(deptStr.trim()));
        }

        Cell costCell = row.getCell(9);                  // ✅ 9 = cost
        if (costCell != null && costCell.getCellType() == CellType.NUMERIC) {
            asset.setCost(BigDecimal.valueOf(costCell.getNumericCellValue()));
        }

        String statusStr = getStringCell(row, 10);       // ✅ 10 = status
        if (statusStr != null && !statusStr.isBlank()) {
            asset.setStatus(AssetStatus.valueOf(statusStr.trim()));
        }

        asset.setStatusNote(getStringCell(row, 11));     // ✅ 11 = statusNote

        // --- Site & Location by name ---
        String siteName = getStringCell(row, 12);        // ✅ 12 = siteName
        String locationName = getStringCell(row, 13);    // ✅ 13 = locationName

        if (siteName != null && !siteName.isBlank()) {
            Site site = siteRepository.findByName(siteName.trim())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Site '" + siteName + "' not found in database"));
            asset.setSite(site);

            if (locationName != null && !locationName.isBlank()) {
                Location location = locationRepository
                        .findByNameAndSite(locationName.trim(), site)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Location '" + locationName + "' not found in site '" + siteName + "'"));
                asset.setLocation(location);
            } else {
                throw new IllegalArgumentException(
                        "Location name is required when site is specified");
            }
        } else {
            throw new IllegalArgumentException(
                    "Site name is required for asset import");
        }

        return asset;
    }


    private String getStringCell(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return null;
    }

    private LocalDate getDateCell(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : null;
        if (text != null && !text.isBlank()) {
            return LocalDate.parse(text); // expects YYYY-MM-DD
        }
        return null;
    }

}
