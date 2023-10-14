package org.icroco.picture.persistence.mapper;

import org.icroco.picture.model.Keyword;
import org.icroco.picture.persistence.model.KeywordEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper
public interface KeywordMapper {
    //    @Mapping(target = "manufacturer", source = "make")
    Keyword toDomain(KeywordEntity mediaFile);

    @InheritInverseConfiguration
    KeywordEntity toEntity(Keyword keyword);
}
