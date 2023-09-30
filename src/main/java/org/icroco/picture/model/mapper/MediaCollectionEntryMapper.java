package org.icroco.picture.model.mapper;

import org.icroco.picture.model.MediaCollectionEntry;
import org.icroco.picture.persistence.model.MediaCollectionEntryEntity;
import org.mapstruct.Builder;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(builder = @Builder(disableBuilder = true))
public interface MediaCollectionEntryMapper {
    //    @Mapping(target = "manufacturer", source = "make")
    MediaCollectionEntry map(MediaCollectionEntryEntity entry);

    @InheritInverseConfiguration
    MediaCollectionEntryEntity map(MediaCollectionEntry entry);
}
