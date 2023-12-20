package org.icroco.picture.persistence.mapper;

import org.icroco.picture.model.MediaFile;
import org.icroco.picture.persistence.model.MediaFileEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.Collection;

@Mapper(uses = { KeywordMapper.class, ThumbnailMapper.class, GeoLocationMapper.class, CameraMapper.class, DimensionMapper.class }
/*, builder = @Builder(disableBuilder = true)*/)
public interface MediaFileMapper {

    //    @Mapping(target = "manufacturer", source = "make")
    MediaFile toDomain(MediaFileEntity mediaFile);

    Collection<MediaFile> toDomains(Collection<MediaFileEntity> mediaFiles);

    @InheritInverseConfiguration(name = "toDomain")
    MediaFileEntity toEntity(MediaFile mediaFile);

    Collection<MediaFileEntity> totoEntities(Collection<MediaFile> mediaFiles);

    MediaFileEntity toEntityFromDomain(MediaFile domain, @MappingTarget MediaFileEntity entity);

    MediaFile toDomainFromEntity(MediaFileEntity entity, @MappingTarget MediaFile domain);

}
