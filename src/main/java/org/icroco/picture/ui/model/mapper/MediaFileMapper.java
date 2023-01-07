package org.icroco.picture.ui.model.mapper;

import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.ui.gallery.GalleryController;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.model.Tag;
import org.icroco.picture.ui.model.ThumbnailImage;
import org.icroco.picture.ui.persistence.DbMediaFile;
import org.icroco.picture.ui.persistence.DbTag;
import org.icroco.picture.ui.util.ImageUtils;
import org.mapstruct.Builder;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;

@Mapper(uses = TagMapper.class, builder = @Builder(disableBuilder = true))
public interface MediaFileMapper {
    Logger log = org.slf4j.LoggerFactory.getLogger(MediaFileMapper.class);

    //    @Mapping(target = "manufacturer", source = "make")
    MediaFile map(DbMediaFile mediaFile);

    default ThumbnailImage map(byte[] rawImage) {
        if (rawImage == null) {
            return new ThumbnailImage(null, false);
        }
//        log.info("Read Image from Database");
        return new ThumbnailImage(ImageUtils.getJavaFXImage(rawImage, 200, 200), true);
    }

    default byte[] map(ThumbnailImage thumbnailImage) {
        return ImageUtils.getRawImage(thumbnailImage.getImage());
    }

    @InheritInverseConfiguration
    DbMediaFile map(MediaFile mediaFile);
}
