package AssetManagement.AssetManagement.enums;

public enum AssetStatus {
    AVAILABLE,
    RESERVED,
    CHECKED_IN,
    CHECKED_OUT,
    IN_REPAIR,
    LOST,
    DISPOSED,
     // This status will trigger the need for a repair note
    ASSIGNED_TO_LOCATION,
    DELETED;
}
