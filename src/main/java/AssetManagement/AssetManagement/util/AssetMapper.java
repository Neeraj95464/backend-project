package AssetManagement.AssetManagement.util;

import AssetManagement.AssetManagement.dto.AssetDTO;
import AssetManagement.AssetManagement.entity.Asset;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    AssetDTO entityToDto(Asset asset);
    Asset dtoToEntity(AssetDTO dto);
}

