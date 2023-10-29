package org.icroco.picture.views.ext_import;

import org.icroco.picture.model.MediaFile;

interface IRenameFilesStrategy {
    default void reset() {
    }

    String computeNewFileName(MediaFile mediaFile);
}
