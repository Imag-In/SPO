package org.icroco.picture.ui.model;

import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.file.Path;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Thumbnail {
    private long           id;
    private Path           fullPath;
    private Image          image;
    private EThumbnailType origin;
    @Builder.Default
    private boolean        embeddedAvailable = false;
}
