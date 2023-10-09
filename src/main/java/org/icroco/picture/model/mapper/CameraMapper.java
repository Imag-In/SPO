package org.icroco.picture.model.mapper;

import org.icroco.picture.model.Camera;
import org.icroco.picture.persistence.model.CameraEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface CameraMapper {
    CameraEntity toEntity(Camera camera);

    Camera toDomain(CameraEntity cameraEntity);
}