package org.icroco.picture.model.mapper;

import org.icroco.picture.model.Tag;
import org.icroco.picture.persistence.model.TagEntity;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper
public interface TagMapper {
//    @Mapping(target = "manufacturer", source = "make")
Tag map(TagEntity mediaFile);

    @InheritInverseConfiguration
    TagEntity map(Tag tag);
}
