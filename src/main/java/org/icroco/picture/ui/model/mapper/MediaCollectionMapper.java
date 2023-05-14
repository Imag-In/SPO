package org.icroco.picture.ui.model.mapper;

import org.icroco.picture.ui.model.MediaCollection;
import org.icroco.picture.ui.persistence.model.DbMediaCollection;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(uses = { MediaCollectionEntryMapper.class, MediaFileMapper.class })
public interface MediaCollectionMapper {
    //    @Mapping(target = "manufacturer", source = "make")
    MediaCollection map(DbMediaCollection catalog);

    @InheritInverseConfiguration
    DbMediaCollection map(MediaCollection mediaCollection);
}
