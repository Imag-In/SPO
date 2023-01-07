package org.icroco.picture.ui.model.mapper;

import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.persistence.DbMediaFile;
import org.icroco.picture.ui.util.ImageUtils;
import org.mapstruct.Builder;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.slf4j.Logger;

@Mapper(uses = TagMapper.class, builder = @Builder(disableBuilder = true))
public interface MediaFileMapper {

    //    @Mapping(target = "manufacturer", source = "make")
    MediaFile map(DbMediaFile mediaFile);

    @InheritInverseConfiguration
    DbMediaFile map(MediaFile mediaFile);
}
