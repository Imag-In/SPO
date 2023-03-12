package org.icroco.picture.ui.model.mapper;

import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.persistence.model.DbMediaFile;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(uses = { TagMapper.class, ThumbnailMapper.class }/*, builder = @Builder(disableBuilder = true)*/)
public interface MediaFileMapper {

    //    @Mapping(target = "manufacturer", source = "make")
    MediaFile map(DbMediaFile mediaFile);

    @InheritInverseConfiguration
    DbMediaFile map(MediaFile mediaFile);


}
