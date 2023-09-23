package org.icroco.picture.model.mapper;

import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.persistence.model.DbMediaCollection;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(uses = { MediaCollectionEntryMapper.class, MediaFileMapper.class })
public interface MediaCollectionMapper {
    //    @Mapping(target = "manufacturer", source = "make")
    MediaCollection map(DbMediaCollection catalog);

    @InheritInverseConfiguration
    DbMediaCollection map(MediaCollection mediaCollection);
}
