package org.icroco.picture.persistence.mapper;

import org.icroco.picture.model.MediaFile;
import org.icroco.picture.persistence.model.MediaFileEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(uses = { TagMapper.class, ThumbnailMapper.class, GeoLocationMapper.class, CameraMapper.class }
/*, builder = @Builder(disableBuilder = true)*/)
public interface MediaFileMapper {

    //    @Mapping(target = "manufacturer", source = "make")
    MediaFile toDomain(MediaFileEntity mediaFile);

    @InheritInverseConfiguration
    MediaFileEntity toEntity(MediaFile mediaFile);


}
