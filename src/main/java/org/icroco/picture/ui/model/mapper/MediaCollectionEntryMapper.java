package org.icroco.picture.ui.model.mapper;

import org.icroco.picture.ui.model.MediaCollectionEntry;
import org.icroco.picture.ui.persistence.model.DbMediaCollectionEntry;
import org.mapstruct.Builder;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(builder = @Builder(disableBuilder = true))
public interface MediaCollectionEntryMapper {
    //    @Mapping(target = "manufacturer", source = "make")
    MediaCollectionEntry map(DbMediaCollectionEntry entry);

    @InheritInverseConfiguration
    DbMediaCollectionEntry map(MediaCollectionEntry entry);
}
