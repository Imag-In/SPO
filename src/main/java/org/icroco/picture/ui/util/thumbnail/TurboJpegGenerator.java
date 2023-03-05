package org.icroco.picture.ui.util.thumbnail;

import org.icroco.picture.ui.util.Dimension;

import java.nio.file.Path;

public class TurboJpegGenerator extends AbstractThumbnailGenerator {

    @Override
    public ThumbnailOutput generateJpg(Path path, Dimension dim) {
        return null;
    }

    @Override
    public void generate(Path source, Path target, Dimension dim) {
        throw new RuntimeException("NotYetImplemented");
    }
}
