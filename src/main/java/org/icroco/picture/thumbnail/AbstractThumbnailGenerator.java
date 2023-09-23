package org.icroco.picture.thumbnail;

import org.icroco.picture.model.Dimension;
import org.icroco.picture.model.Thumbnail;

import java.nio.file.Path;

public abstract class AbstractThumbnailGenerator implements IThumbnailGenerator {
    @Override
    public Thumbnail generate(Path path, Dimension dim) {
        throw new RuntimeException("NotYetImplemented");
    }

    @Override
    public Thumbnail extractThumbnail(Path path) {
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
