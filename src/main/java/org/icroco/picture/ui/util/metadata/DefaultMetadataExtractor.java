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
            return orientation(null, ImageMetadataReader.readMetadata(input));
        }
        catch (ImageProcessingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<MetadataHeader> header(final Path path, final InputStream input) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(input, input.available());
            return Optional.of(new MetadataHeader(originalDateTime(path, metadata).orElse(EPOCH_0),
                                                  orientation(path, metadata).orElse(1)));
        }
        catch (Throwable ex) {
            log.warn("Cannot read header for Path: {}, message: {}", path, ex.getLocalizedMessage());
            return Optional.empty();
        }
    }

//
//    public Optional<byte[]> thumbnail(Path path, Metadata metadata) {
//        return Optional.ofNullable(metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class))
//                       .map(Unchecked.function(this::getThumbnail, throwable -> log.warn("'{}' Cannot read orientation", path)));
//    }


    public Optional<Integer> orientation(Path path, Metadata metadata) {
        return Optional.ofNullable(metadata.getFirstDirectoryOfType(ExifDirectoryBase.class))
                       .map(Unchecked.function(this::getOrientation, throwable -> log.warn("'{}' Cannot read orientation", path)));
    }

    public Optional<LocalDateTime> originalDateTime(Path path, Metadata metadata) {
        return Optional.ofNullable(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class))
                       .map(Unchecked.function(this::getDateTime, throwable -> log.warn("'{}' Cannot read original date time", path)));
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

//    byte[] getThumbnail(ExifThumbnailDirectory directory) {
//        return directory.getObject(TAG_THUMBNAIL_DATA)
//    }
}
