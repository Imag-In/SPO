package org.icroco.picture.ui.util.image;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.icroco.picture.ui.model.MediaFile;
import org.icroco.picture.ui.util.MediaLoader;
import org.icroco.picture.ui.util.hash.IHashGenerator;
import org.icroco.picture.ui.util.metadata.IMetadataExtractor;
import org.icroco.picture.ui.util.thumbnail.ImgscalrGenerator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageLoader {
    private final IHashGenerator     hashGenerator;
    private final IMetadataExtractor metadataExtractor;

//    public record ImageResult(Image)

    public Image loadImage(MediaFile mediaFile) {
        var orientation = metadataExtractor.orientation(mediaFile.getFullPath()).orElse(1);

        var image = new Image(mediaFile.getFullPath().toUri().toString(), MediaLoader.PRIMARY_SCREEN_WIDTH, 0, true, true);
        var bi    = SwingFXUtils.fromFXImage(image, null);

        return SwingFXUtils.toFXImage(ImgscalrGenerator.adaptOrientation(bi, orientation), null);

//        var img         = ImageIO.read(mediaFile.getFullPath().toFile()); // load image

    }
}
