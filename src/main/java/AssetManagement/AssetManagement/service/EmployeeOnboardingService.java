


package AssetManagement.AssetManagement.service;

import AssetManagement.AssetManagement.dto.*;
import AssetManagement.AssetManagement.entity.Location;
import AssetManagement.AssetManagement.entity.Site;
import AssetManagement.AssetManagement.entity.User;
import AssetManagement.AssetManagement.enums.Department;
import AssetManagement.AssetManagement.enums.TicketCategory;
import AssetManagement.AssetManagement.enums.TicketDepartment;
import AssetManagement.AssetManagement.enums.OnboardingStatus;
import AssetManagement.AssetManagement.repository.AssetRepository;
import AssetManagement.AssetManagement.repository.LocationRepository;
import AssetManagement.AssetManagement.repository.SiteRepository;
import AssetManagement.AssetManagement.repository.UserRepository;
import AssetManagement.AssetManagement.util.AuthUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Employee onboarding service:
 * - Reads onboarding Excel,
 * - Creates/updates users,
 * - Raises tickets for CUG SIM, hardware, and email provisioning.
 *
 * Excel column mapping (0-indexed):
 * 0 - employeeId (required)
 * 1 - username
 * 2 - professionalEmail (required)
 * 3 - personalEmail
 * 4 - phoneNumber
 * 5 - department (string -> Department enum)
 * 6 - siteName
 * 7 - locationName
 * 8 - aadharNumber (optional)
 * 9 - panNumber (optional)
 * 10 - note (optional)
 * 11 - cugRequired (yes/no or non-empty string)
 * 12 - isLaptopOrDesktopRequired (yes/no or non-empty)
 * 13 - designation
 * 14 - replacementEmployeeField (free text)
 * 15 - emailProvisionRequired (yes/no or non-empty)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeOnboardingService {

    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final LocationRepository locationRepository;
    private final AssetRepository assetRepository; // kept for possible link usage in future
    private final TicketService ticketService;     // existing service: createTicket(TicketDTO, MultipartFile)

//    @Transactional()
    public OnboardingResultDto processOnboardingExcel(MultipartFile file) {
        int total = 0, success = 0, failed = 0;
        List<OnboardingRowResult> results = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new RuntimeException("Excel sheet is empty");
            }

            // assume first row is header
            int firstDataRow = Math.max(1, sheet.getFirstRowNum() + 1);
            int lastRow = sheet.getLastRowNum();
            total = Math.max(0, lastRow - firstDataRow + 1);

            for (int r = firstDataRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                int displayRowNumber = r + 1; // human-friendly
                try {
                    OnboardingRowResult rowResult = processSingleRow(row, displayRowNumber);
                    results.add(rowResult);

                    if (rowResult.getStatus() == OnboardingStatus.SUCCESS ||
                            rowResult.getStatus() == OnboardingStatus.PARTIAL_SUCCESS) {
                        success++;
                    } else {
                        failed++;
                    }
                } catch (Exception exRow) {
                    log.error("Row {} processing failed: {}", displayRowNumber, exRow.getMessage(), exRow);
                    OnboardingRowResult err = new OnboardingRowResult();
                    err.setRowNumber(displayRowNumber);
                    err.setStatus(OnboardingStatus.FAILED);
                    err.setMessage("Row processing error: " + exRow.getMessage());
                    results.add(err);
                    failed++;
                }
            }

        } catch (Exception e) {
            log.error("Failed to process onboarding file", e);
            results.add(new OnboardingRowResult(0, "N/A", OnboardingStatus.FAILED,
                    "Failed to read file: " + e.getMessage(), Collections.emptyList(), null));
        }

        return new OnboardingResultDto(total, success, failed, results);
    }

    private OnboardingRowResult processSingleRow(Row row, int rowNumber) {
        OnboardingRowResult result = new OnboardingRowResult();
        result.setRowNumber(rowNumber);

        // Parse row into a structured map + DTO-like fields
        Map<String, String> rowMap = readRowIntoMap(row);

        // Validate required fields
        String employeeId = rowMap.get("employeeId");
        String professionalEmail = rowMap.get("professionalEmail");
        String firstUsername = rowMap.get("username");

        if (isBlank(employeeId) || isBlank(professionalEmail)) {
            result.setStatus(OnboardingStatus.FAILED);
            result.setMessage("Missing required fields: employeeId and/or professionalEmail");
            return result;
        }

        try {
            // 1. Create or update user
            User user = createOrUpdateUserFromRow(rowMap);
            result.setCreatedUserId(user.getUsername());
            result.setEmployeeId(user.getEmployeeId());

            // 2. Prepare a base description that copies entire row data (as requested)
            String fullRowDescription = buildFullRowDescription(rowMap);

            // 3. Resolve location id for tickets if possible
            Long locationId = resolveLocationIdByName(rowMap.get("locationName"));

            // 4. Build and raise asset/ticket requirements (CUG SIM, Hardware, Email) as needed
            List<String> createdTicketNumbers = new ArrayList<>();

            // CUG SIM (col 11)
            if (isAffirmative(rowMap.get("cugRequired"))) {
                TicketDTO cug = new TicketDTO();
                cug.setTitle("CUG SIM provisioning for " + employeeId);
                cug.setDescription(fullRowDescription);
                cug.setCategory(TicketCategory.CUG_SIM);
                cug.setTicketDepartment(TicketDepartment.IT);
                cug.setLocation(locationId);
                // ticketService relies on ticketDTO.employee.employeeId to find the user
                UserDTO empDto = new UserDTO();
//                empDto.setEmployeeId(user.getEmployeeId());
//                cug.setEmployee(empDto);

                TicketDTO created = ticketService.createTicket(cug, null);
                if (created != null && created.getId() != null) {
                    createdTicketNumbers.add(String.valueOf(created.getId()));
                }
            }

            // Hardware (col 12)
            if (isAffirmative(rowMap.get("isLaptopOrDesktopRequired"))) {
                TicketDTO hw = new TicketDTO();
                hw.setTitle("Hardware provisioning (Laptop/Desktop) for " + employeeId);
                hw.setDescription(fullRowDescription);
                hw.setCategory(TicketCategory.HARDWARE);
                hw.setTicketDepartment(TicketDepartment.IT);
                hw.setLocation(locationId);
                UserDTO empDto = new UserDTO();
//                empDto.setEmployeeId(user.getEmployeeId());
//                hw.setEmployee(empDto);

                TicketDTO created = ticketService.createTicket(hw, null);
                if (created != null && created.getId() != null) {
                    createdTicketNumbers.add(String.valueOf(created.getId()));
                }
            }

            // Email provisioning (col 15) -> category EMAIL
            if (isAffirmative(rowMap.get("emailProvisionRequired"))) {
                TicketDTO emailTicket = new TicketDTO();
                emailTicket.setTitle("Email provisioning for " + employeeId);
                emailTicket.setDescription(fullRowDescription);
                emailTicket.setCategory(TicketCategory.EMAIL);
                emailTicket.setTicketDepartment(TicketDepartment.IT);
                emailTicket.setLocation(locationId);
                UserDTO empDto = new UserDTO();
//                empDto.setEmployeeId(user.getEmployeeId());
//                emailTicket.setEmployee(empDto);

                TicketDTO created = ticketService.createTicket(emailTicket, null);
                if (created != null && created.getId() != null) {
                    createdTicketNumbers.add(String.valueOf(created.getId()));
                }
            }

            // other ticket types can be added similarly

            result.setCreatedTickets(createdTicketNumbers);
            result.setStatus(OnboardingStatus.SUCCESS);
            result.setMessage("Processed successfully");

        } catch (Exception ex) {
            log.error("Onboarding failed for row {}: {}", rowNumber, ex.getMessage(), ex);
            result.setStatus(OnboardingStatus.FAILED);
            result.setMessage(ex.getMessage());
            result.setCreatedTickets(Collections.emptyList());
        }

        return result;
    }

    private Map<String, String> readRowIntoMap(Row row) {
        Map<String, String> m = new HashMap<>();
        m.put("employeeId", safeGetString(row, 0));
        m.put("username", safeGetString(row, 1));
        m.put("professionalEmail", safeGetString(row, 2));
        m.put("personalEmail", safeGetString(row, 3));
        m.put("phoneNumber", safeGetString(row, 4));
        m.put("department", safeGetString(row, 5));
        m.put("siteName", safeGetString(row, 6));
        m.put("locationName", safeGetString(row, 7));
        m.put("aadharNumber", safeGetString(row, 8));
        m.put("panNumber", safeGetString(row, 9));
        m.put("note", safeGetString(row, 10));
        m.put("cugRequired", safeGetString(row, 11));
        m.put("isLaptopOrDesktopRequired", safeGetString(row, 12));
        m.put("designation", safeGetString(row, 13));
        m.put("replacementEmployee", safeGetString(row, 14));
        m.put("emailProvisionRequired", safeGetString(row, 15));
        return m;
    }

    private String buildFullRowDescription(Map<String, String> row) {
        StringBuilder sb = new StringBuilder();
        sb.append("Onboarding details (copied from sheet):").append(System.lineSeparator());
        sb.append("Employee ID: ").append(nullSafe(row.get("employeeId"))).append(System.lineSeparator());
        sb.append("Username: ").append(nullSafe(row.get("username"))).append(System.lineSeparator());
        sb.append("Professional Email: ").append(nullSafe(row.get("professionalEmail"))).append(System.lineSeparator());
        sb.append("Personal Email: ").append(nullSafe(row.get("personalEmail"))).append(System.lineSeparator());
        sb.append("Phone: ").append(nullSafe(row.get("phoneNumber"))).append(System.lineSeparator());
        sb.append("Department: ").append(nullSafe(row.get("department"))).append(System.lineSeparator());
        sb.append("Site: ").append(nullSafe(row.get("siteName"))).append(System.lineSeparator());
        sb.append("Location: ").append(nullSafe(row.get("locationName"))).append(System.lineSeparator());
        sb.append("Aadhar: ").append(nullSafe(row.get("aadharNumber"))).append(System.lineSeparator());
        sb.append("PAN: ").append(nullSafe(row.get("panNumber"))).append(System.lineSeparator());
        sb.append("Note: ").append(nullSafe(row.get("note"))).append(System.lineSeparator());
        sb.append("CUG Required: ").append(nullSafe(row.get("cugRequired"))).append(System.lineSeparator());
        sb.append("Hardware Required: ").append(nullSafe(row.get("isLaptopOrDesktopRequired"))).append(System.lineSeparator());
        sb.append("Designation: ").append(nullSafe(row.get("designation"))).append(System.lineSeparator());
        sb.append("Replacement Info: ").append(nullSafe(row.get("replacementEmployee"))).append(System.lineSeparator());
        sb.append("Email Provision Required: ").append(nullSafe(row.get("emailProvisionRequired"))).append(System.lineSeparator());
        sb.append("Processed at: ").append(LocalDateTime.now().toString());
        return sb.toString();
    }

    private User createOrUpdateUserFromRow(Map<String, String> row) {
        String employeeId = row.get("employeeId");
        Optional<User> existing = userRepository.findByEmployeeId(employeeId);

        Department deptEnum = mapToDepartmentEnum(row.get("department"));

        Site site = null;
        if (!isBlank(row.get("siteName"))) {
            site = siteRepository.findByName(row.get("siteName"))
                    .orElseThrow(() -> new RuntimeException("Site not found: " + row.get("siteName")));
        }

        Location location = null;
        if (!isBlank(row.get("locationName"))) {
            location = locationRepository.findByName(row.get("locationName"))
                    .orElseThrow(() -> new RuntimeException("Location not found: " + row.get("locationName")));
        }

        if (existing.isPresent()) {
            User u = existing.get();
            // Update fields that come from sheet
            if (!isBlank(row.get("username"))) u.setUsername(row.get("username"));
            if (!isBlank(row.get("professionalEmail"))) u.setEmail(row.get("professionalEmail"));
            if (!isBlank(row.get("personalEmail"))) u.setPersonalEmail(row.get("personalEmail"));
            if (!isBlank(row.get("phoneNumber"))) u.setPhoneNumber(row.get("phoneNumber"));
            if (deptEnum != null) u.setDepartment(deptEnum);
            if (!isBlank(row.get("aadharNumber"))) u.setAadharNumber(row.get("aadharNumber"));
            if (!isBlank(row.get("panNumber"))) u.setPanNumber(row.get("panNumber"));
            if (!isBlank(row.get("note"))) u.setNote(row.get("note"));
            if (site != null) u.setSite(site);
            if (location != null) u.setLocation(location);

            String modifiedBy = AuthUtils.getAuthenticatedUserExactName();
            u.setCreatedBy(modifiedBy);

//            u.setIsDeleted(false);
            User saved = userRepository.save(u);
            log.info("Updated user for employeeId {}", employeeId);
            return saved;
        } else {
            User newUser = new User();
            newUser.setEmployeeId(employeeId);
            newUser.setUsername(firstNonNullOr(row.get("username"), employeeId));
            newUser.setEmail(row.get("professionalEmail"));
            newUser.setPersonalEmail(row.get("personalEmail"));
            newUser.setPhoneNumber(row.get("phoneNumber"));
            newUser.setDepartment(deptEnum);
            newUser.setAadharNumber(row.get("aadharNumber"));
            newUser.setPanNumber(row.get("panNumber"));
            newUser.setNote(row.get("note"));
            newUser.setSite(site);
            newUser.setLocation(location);
            newUser.setDesignation(row.get("designation"));
//            newUser.setIsDeleted(false);
            String createdBy = AuthUtils.getAuthenticatedUserExactName();
            newUser.setCreatedBy(createdBy);

            User saved = userRepository.save(newUser);
            log.info("Created new user for employeeId {}", employeeId);
            return saved;
        }
    }

    private Department mapToDepartmentEnum(String deptStr) {
        if (isBlank(deptStr)) {
            return Department.NA;
        }
        String normalized = deptStr.trim();

        // Try exact match ignoring case/whitespace/underscores
        for (Department d : Department.values()) {
            if (d.name().equalsIgnoreCase(normalized.replace(" ", "_"))) {
                return d;
            }
        }

        // Try contains/startsWith (loose matching) - useful for values like "HR" / "Human Resource"
        String nLower = normalized.toLowerCase();
        for (Department d : Department.values()) {
            String dn = d.name().toLowerCase();
            if (dn.contains(nLower) || nLower.contains(dn)) {
                return d;
            }
        }

        // fallback
        return Department.NA;
    }

    private Long resolveLocationIdByName(String locationName) {
        if (isBlank(locationName)) return null;
        return locationRepository.findByName(locationName)
                .map(Location::getId)
                .orElse(null);
    }

    // --- Helpers for Excel cell reading ---
    private String safeGetString(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;

        CellType t = cell.getCellType();
        if (t == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (t == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            } else {
                double d = cell.getNumericCellValue();
                long l = (long) d;
                if (Double.compare(d, (double) l) == 0) {
                    return String.valueOf(l);
                } else {
                    return String.valueOf(d);
                }
            }
        } else if (t == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (t == CellType.FORMULA) {
            try {
                return cell.getStringCellValue().trim();
            } catch (Exception ignored) {
                try {
                    double d = cell.getNumericCellValue();
                    return String.valueOf((long) d);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean isAffirmative(String value) {
        if (isBlank(value)) return false;
        String v = value.trim().toLowerCase();
        return v.equals("yes") || v.equals("y") || v.equals("true") || v.equals("1") || v.equals("required") || v.equals("req");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String firstNonNullOr(String a, String fallback) {
        return isBlank(a) ? fallback : a;
    }
}
