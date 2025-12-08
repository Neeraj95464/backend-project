package AssetManagement.AssetManagement.mapper;

import AssetManagement.AssetManagement.dto.SimAttachmentDto;
import AssetManagement.AssetManagement.entity.SimAttachment;

public class SimAttachmentMapper {

    public static SimAttachmentDto toDto(SimAttachment entity) {
        if (entity == null) return null;

        SimAttachmentDto dto = new SimAttachmentDto();
        dto.setId(entity.getId());
        dto.setSimId(
                entity.getSimCard() != null ? entity.getSimCard().getId() : null
        );
        dto.setFileName(entity.getFileName());
        dto.setFileType(entity.getFileType());
        dto.setFileSize(entity.getFileSize());
        dto.setFileUrl(entity.getFileUrl());
        dto.setUploadedAt(entity.getUploadedAt());
        dto.setNote(entity.getNote());

        return dto;
    }

    public static SimAttachment toEntity(SimAttachmentDto dto) {
        if (dto == null) return null;

        SimAttachment entity = new SimAttachment();
        entity.setId(dto.getId());
        entity.setFileName(dto.getFileName());
        entity.setFileType(dto.getFileType());
        entity.setFileSize(dto.getFileSize());
        entity.setFileUrl(dto.getFileUrl());
        entity.setUploadedAt(dto.getUploadedAt());
        entity.setNote(dto.getNote());

        return entity;
    }

}

