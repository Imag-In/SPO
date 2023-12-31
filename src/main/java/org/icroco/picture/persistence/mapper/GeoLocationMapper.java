package org.icroco.picture.persistence.mapper;

import org.icroco.picture.model.GeoLocation;
import org.icroco.picture.persistence.model.GeoLocationEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper
public interface GeoLocationMapper {
    //    @Mapping(target = "manufacturer", source = "make")
    GeoLocation toDomain(GeoLocationEntity geoLocation);

    @InheritInverseConfiguration
    GeoLocationEntity toEntity(GeoLocation geoLocation);
}
