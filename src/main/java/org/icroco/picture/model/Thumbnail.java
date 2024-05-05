package org.icroco.picture.model;

import javafx.scene.image.Image;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(of = "mfId")
public class Thumbnail {
    private Long           mfId;
    private Path           fullPath;
    private Image          image;
    private EThumbnailType origin;
    private LocalDateTime  lastUpdate;
    private Dimension      dimension;
    private String lastErrorMessage;
}
