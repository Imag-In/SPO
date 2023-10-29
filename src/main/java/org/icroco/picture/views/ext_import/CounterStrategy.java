package org.icroco.picture.views.ext_import;

import org.apache.commons.io.FilenameUtils;
import org.icroco.picture.model.MediaFile;

class CounterStrategy implements IRenameFilesStrategy {
    private int index = 0;

    @Override
    public void reset() {
        index = 0;
    }

    @Override
    public String computeNewFileName(MediaFile mediaFile) {
        return ++index + "." + FilenameUtils.getExtension(mediaFile.fullPath().toString());
    }
}
