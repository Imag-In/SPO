package org.icroco.picture.ui.util.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.TimeZone;

public class DefaultMetadataExtractor implements IMetadataExtractor {

    public static final  LocalDateTime EPOCH_0 = Instant.ofEpochMilli(0).atZone(ZoneId.systemDefault()).toLocalDateTime();
    private static final Logger        log     = org.slf4j.LoggerFactory.getLogger(DefaultMetadataExtractor.class);

    @Override
    public Optional<Integer> orientation(InputStream input) {
        try {
            return orientation(ImageMetadataReader.readMetadata(input));
        }
        catch (ImageProcessingException | IOException | MetadataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<MetadataHeader> header(Path path, InputStream input) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(input, input.available());
            return Optional.of(new MetadataHeader(originalDateTime(metadata), orientation(metadata).orElse(1)));
        }
        catch (Throwable ex) {
            log.warn("Cannot read header for Path: {}, message: {}", path, ex.getLocalizedMessage());
            return Optional.empty();
        }
    }

    public Optional<Integer> orientation(Metadata metadata) throws MetadataException {
        return Optional.ofNullable(metadata.getFirstDirectoryOfType(ExifDirectoryBase.class))
                       .map(Unchecked.function(this::getOrientation));
    }

    public LocalDateTime originalDateTime(Metadata metadata) throws MetadataException {
        return Unchecked.function((Metadata md) -> getDateTime(md.getFirstDirectoryOfType(ExifSubIFDDirectory.class))).apply(metadata);
    }

    Integer getOrientation(Directory directory) throws MetadataException {

        if (directory.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
            return directory.getInt(ExifDirectoryBase.TAG_ORIENTATION);
        }
        return 1;
    }

    LocalDateTime getDateTime(ExifSubIFDDirectory directory) {
        if (directory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
            return LocalDateTime.ofInstant(directory.getDateOriginal(TimeZone.getDefault()).toInstant(), ZoneId.systemDefault());
        }
        return EPOCH_0;
    }
}
