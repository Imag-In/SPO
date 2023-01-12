package org.icroco.picture.ui.util.thumbnail;

import java.nio.file.Path;

public abstract class AbstractThumbnailGenerator implements IThumbnailGenerator {

    protected void deleteFile(Path p) {
        final var file = p.toFile();
        if (file.isFile() && file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException("Cannot delete file: " + p);
            }
        }
    }
}
