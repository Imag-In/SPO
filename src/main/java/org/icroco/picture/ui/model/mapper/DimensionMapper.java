package org.icroco.picture.ui.model.mapper;

import org.icroco.picture.ui.model.Dimension;
import org.icroco.picture.ui.persistence.model.DbDimension;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper
public interface DimensionMapper {
    //    @Mapping(target = "manufacturer", source = "make")
    Dimension map(DbDimension dimension);

    @InheritInverseConfiguration
    DbDimension map(Dimension dimension);
}
