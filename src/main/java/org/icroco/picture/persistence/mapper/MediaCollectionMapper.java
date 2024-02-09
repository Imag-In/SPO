package org.icroco.picture.persistence.mapper;

import org.icroco.picture.model.MediaCollection;
import org.icroco.picture.persistence.model.MediaCollectionEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(uses = { MediaFileMapper.class })
public interface MediaCollectionMapper {
    //    @Mapping(target = "manufacturer", source = "make")
    MediaCollection toDomain(MediaCollectionEntity catalog);

    @InheritInverseConfiguration
    MediaCollectionEntity toEntity(MediaCollection mediaCollection);
}
