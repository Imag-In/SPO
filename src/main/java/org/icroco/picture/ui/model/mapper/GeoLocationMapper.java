package org.icroco.picture.ui.model.mapper;

import org.icroco.picture.ui.model.GeoLocation;
import org.icroco.picture.ui.persistence.model.DbGeoLocation;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper
public interface GeoLocationMapper {
    //    @Mapping(target = "manufacturer", source = "make")
    GeoLocation map(DbGeoLocation geoLocation);

    @InheritInverseConfiguration
    DbGeoLocation map(GeoLocation geoLocation);
}
