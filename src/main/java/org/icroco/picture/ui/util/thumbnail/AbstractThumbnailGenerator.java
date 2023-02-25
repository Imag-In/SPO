package org.icroco.picture.ui.util.thumbnail;

import javafx.scene.image.Image;

import java.nio.file.Path;

public abstract class AbstractThumbnailGenerator implements IThumbnailGenerator {

    @Override
    public Image extractThumbnail(Path path) {
        throw new RuntimeException("NotYetImplemented");
    }

    protected void deleteFile(Path p) {
        final var file = p.toFile();
        if (file.isFile() && file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException("Cannot delete file: " + p);
            }
        }
    }
}
