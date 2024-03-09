package org.icroco.picture.views.ext_import;

import org.apache.commons.io.FilenameUtils;
import org.icroco.picture.model.MediaFile;
import org.springframework.stereotype.Component;

@Component
public final class CounterStrategy implements IRenameFilesStrategy {
    private int index = 0;

    @Override
    public void reset() {
        index = 0;
    }

    @Override
    public String displayName() {
        return "COUNTER";
    }

    @Override
    public String computeNewFileName(MediaFile mediaFile) {
        return STR."\{++index}.\{FilenameUtils.getExtension(mediaFile.fullPath().toString())}";
    }

    @Override
    public String getI18NId() {
        return "rename.file.strategy.counter";
    }
}
