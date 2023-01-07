package org.icroco.picture.ui.model.mapper;

import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Tag;
import org.icroco.picture.ui.persistence.DbMediaFile;
import org.icroco.picture.ui.persistence.DbTag;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper
public interface TagMapper {
//    @Mapping(target = "manufacturer", source = "make")
    Tag map(DbTag mediaFile);

    @InheritInverseConfiguration
    DbTag map(Tag tag);
}
