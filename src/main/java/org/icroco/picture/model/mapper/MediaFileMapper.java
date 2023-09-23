package org.icroco.picture.model.mapper;

import org.icroco.picture.model.MediaFile;
import org.icroco.picture.persistence.model.DbMediaFile;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(uses = { TagMapper.class, ThumbnailMapper.class, GeoLocationMapper.class }/*, builder = @Builder(disableBuilder = true)*/)
public interface MediaFileMapper {

    //    @Mapping(target = "manufacturer", source = "make")
    MediaFile map(DbMediaFile mediaFile);

    @InheritInverseConfiguration
    DbMediaFile map(MediaFile mediaFile);


}
