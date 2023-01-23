package org.icroco.picture.ui.model.mapper;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import org.icroco.picture.ui.model.EThumbnailStatus;
import org.icroco.picture.ui.model.Thumbnail;
import org.icroco.picture.ui.persistence.model.DbThumbnail;
import org.icroco.picture.ui.util.ImageUtils;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;

@Mapper
public abstract class ThumbnailMapper {

    //    @Mapping(target = "manufacturer", source = "make")
    public abstract Thumbnail map(DbThumbnail thumbnail);

    @InheritInverseConfiguration
    public abstract DbThumbnail map(Thumbnail thumbnail);

    public final SimpleObjectProperty<EThumbnailStatus> map(EThumbnailStatus status) {
        return new SimpleObjectProperty<>(status);
    }

    public final EThumbnailStatus map(SimpleObjectProperty<EThumbnailStatus> status) {
        return status.get();
    }

    public final Image map(byte[] array) {
        return array == null ? null : ImageUtils.mapAsJpg(array);
    }

    public final byte[] map(Image image) {
        return image == null ? null : ImageUtils.mapAsJpg(image);
    }


}
