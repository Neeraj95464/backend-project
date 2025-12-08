//package AssetManagement.AssetManagement.service;
//
//import AssetManagement.AssetManagement.dto.RowError;
//import AssetManagement.AssetManagement.dto.SimCardRequestDto;
//import AssetManagement.AssetManagement.dto.SimImportResult;
//import AssetManagement.AssetManagement.entity.Location;
//import AssetManagement.AssetManagement.entity.Site;
//import AssetManagement.AssetManagement.enums.SimProvider;
//import AssetManagement.AssetManagement.enums.SimStatus;
//import AssetManagement.AssetManagement.service.SimCardService;
//import AssetManagement.AssetManagement.repository.LocationRepository;
//import AssetManagement.AssetManagement.repository.SiteRepository;
//
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.apache.poi.ss.usermodel.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.InputStream;
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class SimCardImportService {
//
//    private final SimCardService simCardService;
//    private final SiteRepository siteRepository;
//    private final LocationRepository locationRepository;
//
//    @Transactional
//    public SimImportResult importSimsFromExcel(MultipartFile file) {
//
//        int success = 0;
//        List<RowError> errors = new ArrayList<>();
//
//        try (InputStream is = file.getInputStream();
//             Workbook workbook = WorkbookFactory.create(is)) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//
//            // Skip header row
//            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
//
//                Row row = sheet.getRow(rowIdx);
//                if (row == null) continue;
//
//                try {
//                    SimCardRequestDto req = mapRowToSim(row);
//                    simCardService.createSimCard(req);
//                    success++;
//                } catch (Exception ex) {
//                    errors.add(new RowError(rowIdx + 1, ex.getMessage()));
//                }
//            }
//        } catch (Exception e) {
//            errors.add(new RowError(0, "Failed to read file: " + e.getMessage()));
//        }
//
//        return new SimImportResult(success, errors.size(), errors);
//    }
//
//
//    private SimCardRequestDto mapRowToSim(Row row) {
//
//        SimCardRequestDto dto = new SimCardRequestDto();
//
//        dto.setPhoneNumber(getStringCell(row, 0));     // Column A
//        dto.setIccid(getStringCell(row, 1));           // Column B
//        dto.setImsi(getStringCell(row, 2));            // Column C
//
//        String providerStr = getStringCell(row, 3);    // Column D
//        if (providerStr != null)
//            dto.setProvider(SimProvider.valueOf(providerStr.trim()));
//
//        String statusStr = getStringCell(row, 4);      // Column E
//        if (statusStr != null)
//            dto.setStatus(SimStatus.valueOf(statusStr.trim()));
//
//        dto.setPurchaseDate(getDateCell(row, 5));      // Column F
//        dto.setPurchaseFrom(getStringCell(row, 6));    // Column G
//
//        String costStr = getStringCell(row, 7);        // Column H
//        if (costStr != null)
//            dto.setCost(new BigDecimal(costStr));
//
//        String siteName = getStringCell(row, 8);       // Column I
//        String locationName = getStringCell(row, 9);   // Column J
//
//        dto.setSiteName(siteName);
//        dto.setLocationName(locationName);
//
//        // Validate site + location
//        Site site = siteRepository.findByName(siteName)
//                .orElseThrow(() -> new RuntimeException("Site not found: " + siteName));
//        Location location = locationRepository.findByNameAndSite(locationName, site)
//                .orElseThrow(() -> new RuntimeException("Location '" + locationName + "' not found in site '" + siteName + "'"));
//
//        dto.setNote(getStringCell(row, 10));           // Column K (optional)
//
//        return dto;
//    }
//
//    private String getStringCell(Row row, int idx) {
//        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
//        if (cell == null) return null;
//
//        return switch (cell.getCellType()) {
//            case STRING -> cell.getStringCellValue().trim();
//            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
//            default -> null;
//        };
//    }
//
//    private LocalDate getDateCell(Row row, int idx) {
//        Cell cell = row.getCell(idx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
//        if (cell == null) return null;
//
//        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
//            return cell.getLocalDateTimeCellValue().toLocalDate();
//
//        if (cell.getCellType() == CellType.STRING)
//            return LocalDate.parse(cell.getStringCellValue().trim());
//
//        return null;
//    }
//}



package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.RowError;
import AssetManagement.AssetManagement.dto.SimCardRequestDto;
import AssetManagement.AssetManagement.dto.SimImportResult;
import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.SimProvider;
import AssetManagement.AssetManagement.enums.SimStatus;
import AssetManagement.AssetManagement.exception.UserNotFoundException;
import AssetManagement.AssetManagement.repository.LocationRepository;
import AssetManagement.AssetManagement.repository.SiteRepository;

import AssetManagement.AssetManagement.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SimCardImportService {

    private final SimCardService simCardService;
    private final SiteRepository siteRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;


    public SimImportResult importSimsFromExcel(MultipartFile file) {

        int success = 0;
        List<RowError> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {

                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                try {
                    SimCardRequestDto dto = mapRowToSim(row);
                    simCardService.createSimCard(dto);
                    success++;

                } catch (Exception ex) {
                    errors.add(new RowError(rowIdx + 1,
                            ex.getMessage() != null ? ex.getMessage() : "Unknown error"));
                }
            }

        } catch (Exception e) {
            errors.add(new RowError(0, "Failed to read file: " + e.getMessage()));
        }

        return new SimImportResult(success, errors.size(), errors);
    }

//    private SimCardRequestDto mapRowToSim(Row row) {
//
//        SimCardRequestDto dto = new SimCardRequestDto();
//
//        dto.setPhoneNumber(getStringCell(row, 0));
//        dto.setIccid(getStringCell(row, 1));
//        dto.setImsi(getStringCell(row, 2));
//
//        // provider
//        String providerStr = getStringCell(row, 3);
//        if (providerStr != null && !providerStr.isBlank()) {
//            dto.setProvider(SimProvider.valueOf(providerStr.trim().toUpperCase()));
//        }
//
//        // status
//        String statusStr = getStringCell(row, 4);
//        if (statusStr != null && !statusStr.isBlank()) {
//            dto.setStatus(SimStatus.valueOf(statusStr.trim().toUpperCase()));
//        }
//
//        // activatedAt
//        dto.setActivatedAt(getDateCell(row, 5));
//
//        // purchaseDate
//        dto.setPurchaseDate(getDateCell(row, 6));
//
//        // purchaseFrom
//        dto.setPurchaseFrom(getStringCell(row, 7));
//
//        // cost
//        dto.setCost(getBigDecimalCell(row, 8));
//
//        // note
//        dto.setNote(getStringCell(row, 9));
//        System.out.println("Note was "+getStringCell(row,9));
//
//        // site + location
//        String siteName = getStringCell(row, 10);
//        System.out.println("site name was "+siteName);
//        String locationName = getStringCell(row, 11);
//
//        Site site = siteRepository.findByName(siteName)
//                .orElseThrow(() -> new RuntimeException("Site not found: " + siteName));
//
//        Location location = locationRepository.findByNameAndSite(locationName, site)
//                .orElseThrow(() -> new RuntimeException(
//                        "Location '" + locationName + "' not found in site '" + siteName + "'"
//                ));
//
//        dto.setSiteName(siteName);
//        dto.setLocationName(locationName);
//
//        User optionalUser = userRepository
//                .findByEmployeeId(getStringCell(row, 12))
//                        .orElseThrow(()->new UserNotFoundException("User not found "));
//
//        // assigned user ID (optional)
//        dto.setAssignedUserId(optionalUser.getId());
//
//        return dto;
//    }

    private SimCardRequestDto mapRowToSim(Row row) {
        SimCardRequestDto dto = new SimCardRequestDto();

        // ✅ FIXED: Proper column reading (0-based index)
        dto.setPhoneNumber(getStringCell(row, 0));        // A - phoneNumber
        dto.setIccid(getStringCell(row, 1));              // B - iccid
        dto.setImsi(getStringCell(row, 2));               // C - imsi
        dto.setProvider(getEnumCell(row, 3, SimProvider.class));  // D - provider
        dto.setStatus(getEnumCell(row, 4, SimStatus.class));      // E - status
        dto.setActivatedAt(getDateCell(row, 5));          // F - activatedAt
        dto.setPurchaseDate(getDateCell(row, 6));         // G - purchaseDate
        dto.setPurchaseFrom(getStringCell(row, 7));       // H - purchaseFrom
        dto.setCost(getBigDecimalCell(row, 8));           // I - cost
        dto.setNote(getStringCell(row, 9));               // J - note

        String siteName = getStringCell(row, 10);         // K - siteName
        String locationName = getStringCell(row, 11);     // L - locationName
        String employeeId = getStringCell(row, 12);       // M - assignedUserId

//        System.out.println("📊 Row data: site=" + siteName + ", loc=" + locationName + ", emp=" + employeeId);

        // ✅ Site/Location validation
        if (siteName != null && !siteName.isBlank()) {
            Site site = siteRepository.findByName(siteName.trim())
                    .orElseThrow(() -> new RuntimeException("Site not found: '" + siteName + "'"));

            if (locationName != null && !locationName.isBlank()) {
                Location location = locationRepository.findByNameAndSite(locationName.trim(), site)
                        .orElseThrow(() -> new RuntimeException("Location '" + locationName + "' not found in site '" + siteName + "'"));
                dto.setLocationName(locationName.trim());
            }
            dto.setSiteName(siteName.trim());
        }

        // ✅ Optional user assignment
        if (employeeId != null && !employeeId.isBlank()) {
            User user = userRepository.findByEmployeeId(employeeId.trim())
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + employeeId));
            dto.setAssignedUserId(user.getId());
        }

        return dto;
    }

    // ✅ FIXED: Proper string cell reading (NO cell.setCellType!)
    private String getStringCell(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    // ✅ NEW: Enum cell helper
    private <E extends Enum<E>> E getEnumCell(Row row, int index, Class<E> enumClass) {
        String value = getStringCell(row, index);
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid enum value '" + value + "' at column " + (index + 1));
        }
    }

    // ✅ FIXED: BigDecimal (no cell.setCellType)
    private BigDecimal getBigDecimalCell(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.STRING) {
                return new BigDecimal(cell.getStringCellValue().trim());
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid number '" + cell + "' at column " + (index + 1));
        }
        return null;
    }


//    private String getStringCell(Row row, int index) {
//        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
//        if (cell == null) return null;
//
//        cell.setCellType(CellType.STRING); // force convert to string
//        return cell.getStringCellValue().trim();
//    }
//
//    private BigDecimal getBigDecimalCell(Row row, int index) {
//        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
//        if (cell == null) return null;
//
//        try {
//            if (cell.getCellType() == CellType.NUMERIC) {
//                return BigDecimal.valueOf(cell.getNumericCellValue());
//            }
//            cell.setCellType(CellType.STRING);
//            return new BigDecimal(cell.getStringCellValue().trim());
//        } catch (Exception e) {
//            throw new RuntimeException("Invalid number at column " + (index + 1));
//        }
//    }

    private LocalDate getDateCell(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }

            if (cell.getCellType() == CellType.STRING) {
                return LocalDate.parse(cell.getStringCellValue().trim());
            }

        } catch (Exception e) {
            throw new RuntimeException("Invalid date at column " + (index + 1));
        }

        return null;
    }
}
