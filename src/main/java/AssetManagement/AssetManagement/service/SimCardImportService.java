

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
import AssetManagement.AssetManagement.repository.SimCardRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SimCardImportService {

    private final SimCardService simCardService;
    private final SiteRepository siteRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final SimCardRepository simCardRepository;


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

    private SimCardRequestDto mapRowToSim(Row row) {
        SimCardRequestDto dto = new SimCardRequestDto();

        // ✅ FIXED: Proper column reading (0-based index)
        dto.setPhoneNumber(getStringCell(row, 0));        // A - phoneNumber
        String phoneNumber = getStringCell(row, 0);
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            if (simCardRepository.existsByPhoneNumber(phoneNumber.trim())) {
                throw new RuntimeException("❌ Duplicate phone number '" + phoneNumber + "' already exists (Row " + (row.getRowNum() + 1) + ")");
            }
            dto.setPhoneNumber(phoneNumber.trim());
        }

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
//        if (employeeId != null && !employeeId.isBlank()) {
//
//            User user = userRepository.findByEmployeeId(employeeId.trim())
//                    .orElseThrow(() -> new UserNotFoundException("User not found: " + employeeId));
//            dto.setAssignedUserId(user.getId());
//        }


// ✅ With this auto-create logic:
        if (employeeId != null && !employeeId.isBlank()) {
            String cleanEmployeeId = employeeId.trim();
            Optional<User> existingUser = userRepository.findByEmployeeId(cleanEmployeeId);

            if (existingUser.isPresent()) {
                dto.setAssignedUserId(existingUser.get().getEmployeeId());
            } else {
                // Auto-create missing user
                String newUserId = createSystemUserForImport(cleanEmployeeId, row);
                dto.setAssignedUserId(newUserId);
            }
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


    private String createSystemUserForImport(String employeeId, Row row) {
        User systemUser = new User();
        systemUser.setEmployeeId(employeeId);
        systemUser.setUsername(employeeId);  // System-generated
        systemUser.setRole("USER");                   // Default role
        systemUser.setPhoneNumber(getStringCell(row, 0));  // From SIM phone
        systemUser.setPersonalEmail(employeeId + "@system.infradesk");  // Temp email

        // ✅ Audit trail in note
        String siteName = getStringCell(row, 10);
        String locationName = getStringCell(row, 11);
        systemUser.setNote(String.format(
                "🔄 AUTO-CREATED by SIM import | Row %d | Site: %s | Location: %s | UPDATE with real data",
                row.getRowNum() + 1, siteName, locationName
        ));

        // Default values
        systemUser.setDesignation("Employee");
        systemUser.setDeleted(false);
        systemUser.setCreatedBy("SYSTEM_IMPORT");

        User savedUser = userRepository.save(systemUser);
//        log.info("👤 Created system user for employeeId: {} (ID: {})", employeeId, savedUser.getId());

        return savedUser.getEmployeeId();
    }

}
