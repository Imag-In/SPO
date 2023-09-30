package org.icroco.picture.views.util.image;

import javafx.beans.property.DoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.icroco.picture.hash.IHashGenerator;
import org.icroco.picture.metadata.IMetadataExtractor;
import org.icroco.picture.model.MediaFile;
import org.icroco.picture.thumbnail.ImgscalrGenerator;
import org.icroco.picture.views.util.MediaLoader;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageLoader {
    private final IHashGenerator     hashGenerator;
    private final IMetadataExtractor metadataExtractor;

//    public record ImageResult(Image)

    public Image loadImage(MediaFile mediaFile, @Nullable DoubleProperty progressIndicator) {
        // TODO: ORIENTATION SHOULD BE IN MEDIA FILE.
        var orientation = metadataExtractor.orientation(mediaFile.getFullPath()).orElse(1);

//        var image = new Image(mediaFile.getFullPath().toUri().toString(), MediaLoader.PRIMARY_SCREEN_WIDTH, 0, true, true);
        var image = new Image(mediaFile.getFullPath().toUri().toString(), MediaLoader.PRIMARY_SCREEN_WIDTH, 0, true, true, true);
//        image.setRotate(90);
        if (progressIndicator != null) {
            progressIndicator.bind(image.progressProperty());
        }

        var bi      = SwingFXUtils.fromFXImage(image, null);
        var fxImage = SwingFXUtils.toFXImage(ImgscalrGenerator.adaptOrientation(bi, orientation), null);
        if (progressIndicator != null) {
            progressIndicator.isBound();
        }
        return fxImage;

    }

    public Image loadImage(MediaFile mediaFile) {
        return loadImage(mediaFile, null);
    }
}
