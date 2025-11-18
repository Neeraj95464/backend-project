package AssetManagement.AssetManagement.util;

import AssetManagement.AssetManagement.enums.Department;
import AssetManagement.AssetManagement.enums.TicketDepartment;


public class TicketDepartmentMapper {

    public static TicketDepartment map(Department department) {
        if (department == null) {
            return null;
        }
        switch (department) {
            case IT:
                return TicketDepartment.IT;  // Example mapping
            case HR:
                return TicketDepartment.HR;
//            case SALES:
//                return TicketDepartment.SALES;
            // Add more mappings according to your enums
            default:
                throw new IllegalArgumentException("Unknown Department: " + department);
        }
    }
}
