package org.icroco.picture.views.ext_import;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.icroco.picture.model.MediaFile;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Getter(value = AccessLevel.PROTECTED)
abstract class AbstractDateStrategy implements IRenameFilesStrategy {
    private final Set<String>       cache = new HashSet<>();
    private final DateTimeFormatter formatter;


    @Override
    public String computeNewFileName(MediaFile mediaFile) {
        var valueOriginal = formatter.format(mediaFile.getOriginalDate());
        var value         = valueOriginal;

        int idx = 0;
        while (cache.contains(value)) {
            value = valueOriginal + "-" + (++idx);
        }
        cache.add(value);
        return value + "." + FilenameUtils.getExtension(mediaFile.fullPath().toString());
    }
}
