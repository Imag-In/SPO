package org.icroco.picture.ui.util.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectoryBase;
import lombok.extern.slf4j.Slf4j;
import org.jooq.lambda.Unchecked;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class DefaultMetadataExtractor implements IMetadataExtractor {
    @Override
    public Optional<Integer> orientation(Path path) {
        try {
            return orientation(ImageMetadataReader.readMetadata(path.toFile()));
        }
        catch (ImageProcessingException | IOException | MetadataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Integer> orientation(InputStream input) {
        try {
            return orientation(ImageMetadataReader.readMetadata(input));
        }
        catch (ImageProcessingException | IOException | MetadataException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Integer> orientation(Metadata metadata) throws MetadataException {
        return Optional.ofNullable(metadata.getFirstDirectoryOfType(ExifDirectoryBase.class))
                       .map(d -> Unchecked.function(this::getOrientation).apply(d));
    }

    Integer getOrientation(Directory directory) throws MetadataException {

        if (directory.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
            return directory.getInt(ExifDirectoryBase.TAG_ORIENTATION);
        }
        return 1;

    }
}
