package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.RowError;
import AssetManagement.AssetManagement.dto.UserImportResult;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.sql.JDBCType.NUMERIC;

@Service
@RequiredArgsConstructor
public class UserPreImportService {

    private final UserRepository userRepository;

    public UserImportResult validateAndCreateUsers(MultipartFile file) {
        int created = 0, skipped = 0;
        List<String> createdUsers = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                try {
                    String employeeId = getStringCell(row, 12);  // Column M
                    if (employeeId != null && !employeeId.trim().isBlank()) {
                        processEmployeeId(employeeId.trim(), createdUsers, created, skipped);
                    }
                    skipped++;  // Count even if no employeeId
                } catch (Exception ex) {
                    errors.add(new RowError(rowIdx + 1, "User processing failed: " + ex.getMessage()));
                }
            }
        } catch (Exception e) {
            errors.add(new RowError(0, "Failed to read file: " + e.getMessage()));
        }

        return new UserImportResult(created, skipped, createdUsers, errors);
    }

    private void processEmployeeId(String employeeId, List<String> createdUsers, int created, int skipped) {
        if (userRepository.findByEmployeeId(employeeId).isPresent()) {
            // ✅ User exists - skip
            return;
        }

        // ✅ Create missing user
        User newUser = createSystemUser(employeeId);
        userRepository.save(newUser);
        createdUsers.add(employeeId);
    }

    private User createSystemUser(String employeeId) {
        User user = new User();
        user.setEmployeeId(employeeId);
        user.setUsername("emp_" + employeeId);
        user.setRole("USER");
        user.setDesignation("Employee");
        user.setDeleted(false);
        user.setCreatedBy("SIM_PRE_IMPORT");
        user.setNote("👤 AUTO-CREATED by SIM pre-import | Update personal details");
        return user;
    }

    private String getStringCell(Row row, int index) {
        // Same as your SimCardImportService method
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }
}
