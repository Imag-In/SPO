package org.icroco.picture.views.ext_import;

import org.icroco.picture.model.MediaFile;

public interface IRenameFilesStrategy {

    String displayName();

    String computeNewFileName(MediaFile mediaFile);

    default void reset() {
    }
}
