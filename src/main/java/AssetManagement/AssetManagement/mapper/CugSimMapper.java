package AssetManagement.AssetManagement.mapper;

import AssetManagement.AssetManagement.dto.SimCardResponseDto;
import AssetManagement.AssetManagement.entity.SimCard;

public class CugSimMapper {

    public static SimCardResponseDto toDTO(SimCard s) {
        SimCardResponseDto r = new SimCardResponseDto();
        r.setId(s.getId());
//        r.setSimTag(s.getSimTag());
        r.setAssignmentUploaded(s.getAssignmentUploaded());
        r.setPhoneNumber(s.getPhoneNumber());
        r.setIccid(s.getIccid());
        r.setImsi(s.getImsi());
        r.setProvider(s.getProvider());
        r.setStatus(s.getStatus());

        if (s.getAssignedUser() != null) {
            r.setAssignedUserId(s.getAssignedUser().getId());
            r.setAssignedUserName(s.getAssignedUser().getUsername());
        }

        r.setAssignedAt(s.getAssignedAt());
        r.setActivatedAt(s.getActivatedAt());
        r.setPurchaseDate(s.getPurchaseDate());
        r.setPurchaseFrom(s.getPurchaseFrom());
        r.setCost(s.getCost());
        r.setLocationName(s.getLocation() != null ? s.getLocation().getName() : null);
        r.setSiteName(s.getSite() != null ? s.getSite().getName() : null);
        r.setNote(s.getNote());
        r.setCreatedAt(s.getCreatedAt());

        return r;
    }
}

