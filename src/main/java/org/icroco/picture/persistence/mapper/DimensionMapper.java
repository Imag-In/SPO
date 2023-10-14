package org.icroco.picture.persistence.mapper;

import org.icroco.picture.model.Dimension;
import org.icroco.picture.persistence.model.DimensionEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper
public interface DimensionMapper {
    //    @Mapping(target = "manufacturer", source = "make")
    Dimension toDomain(DimensionEntity dimension);

    @InheritInverseConfiguration
    DimensionEntity toEntity(Dimension dimension);
}
