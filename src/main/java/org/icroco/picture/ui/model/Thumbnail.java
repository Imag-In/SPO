package org.icroco.picture.ui.model;

import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

@Data
@Builder
@AllArgsConstructor
public class Thumbnail {
    private long           id;
    private Path           fullPath;
    private Image          image;
    private EThumbnailType origin;
    @Builder.Default
    private boolean        embeddedAvailable = false;
}
