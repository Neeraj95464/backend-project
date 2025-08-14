package AssetManagement.AssetManagement.util;

import AssetManagement.AssetManagement.entity.Asset;
import AssetManagement.AssetManagement.enums.AssetStatus;
import AssetManagement.AssetManagement.enums.AssetType;
import AssetManagement.AssetManagement.enums.Department;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AssetSpecification {

    public static Specification<Asset> hasStatus(AssetStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Asset> hasAssetType(AssetType type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("assetType"), type);
    }

    public static Specification<Asset> hasDepartment(Department dept) {
        return (root, query, cb) ->
                dept == null ? null : cb.equal(root.get("department"), dept);
    }

    public static Specification<Asset> createdBy(String createdBy) {
        return (root, query, cb) ->
                (createdBy == null || createdBy.isBlank()) ? null :
                        cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%");
    }

    public static Specification<Asset> hasSite(Long siteId) {
        return (root, query, cb) ->
                siteId == null ? null : cb.equal(root.get("site").get("id"), siteId);
    }

    public static Specification<Asset> hasLocation(Long locationId) {
        return (root, query, cb) ->
                locationId == null ? null : cb.equal(root.get("location").get("id"), locationId);
    }

    public static Specification<Asset> purchaseDateBetween(LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return (root, query, cb) -> cb.between(root.get("purchaseDate"), start, end);
        }
        if (start != null) {
            return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("purchaseDate"), start);
        }
        if (end != null) {
            return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("purchaseDate"), end);
        }
        return null;
    }

    public static Specification<Asset> createdAtBetween(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return (root, query, cb) -> cb.between(root.get("createdAt"), start, end);
        }
        if (start != null) {
            return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), start);
        }
        if (end != null) {
            return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), end);
        }
        return null;
    }

    public static Specification<Asset> keywordSearch(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), likePattern),
                    cb.like(cb.lower(root.get("serialNumber")), likePattern),
                    cb.like(cb.lower(root.get("brand")), likePattern),
                    cb.like(cb.lower(root.get("model")), likePattern),
                    cb.like(cb.lower(root.get("description")), likePattern)
            );
        };
    }
}

