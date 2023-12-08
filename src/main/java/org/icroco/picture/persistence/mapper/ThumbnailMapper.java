package org.icroco.picture.persistence.mapper;

import javafx.scene.image.Image;
import org.icroco.picture.model.Thumbnail;
import org.icroco.picture.persistence.model.ThumbnailEntity;
import org.icroco.picture.views.util.ImageUtils;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper(uses = { DimensionMapper.class })
public abstract class ThumbnailMapper {

    //    @Mapping(target = "manufacturer", source = "make")
    public abstract Thumbnail toDomain(ThumbnailEntity thumbnail);

    @InheritInverseConfiguration
    public abstract ThumbnailEntity toEntity(Thumbnail thumbnail);

//    public final SimpleObjectProperty<EThumbnailType> map(EThumbnailType status) {
//        return new SimpleObjectProperty<>(status);
//    }

//    public final EThumbnailType map(SimpleObjectProperty<EThumbnailType> status) {
//        return status.get();
//    }

    public final Image map(byte[] array) {
        return array == null ? null : ImageUtils.mapAsJpg(array);
    }

    public final byte[] map(Image image) {
        return image == null ? null : ImageUtils.mapAsJpg(image);
    }


}
