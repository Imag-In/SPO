package org.icroco.picture.metadata;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

@Component
@Slf4j
public class ApacheMetadataWriter implements IMetadataWriter {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US);

    @Override
    @SneakyThrows
    public void setOrignialDate(Path path, LocalDateTime date) {
        // WARN: Could use a lot of memory, but mandaroty with Imaging, otherwize we need to copy the file.
        byte[] source = Files.readAllBytes(path);

        addExifTags(source, path, List.of(dir -> updateOriginalDate(dir, date)));
    }

    @SneakyThrows
    private void updateOriginalDate(TiffOutputDirectory dir, LocalDateTime date) {
        dir.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        dir.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
        var text = formatter.format(date);
        dir.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, text);
        dir.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, text);
    }

    @Override
    public void setOrientation(Path path, int orientation) {

    }

    @Override
    public void setKeywords(Path path, Set<String> keywords) {

    }

    @Override
    public void addKeywords(Path path, Set<String> keywords) {

    }

    @Override
    public void removeKeywords(Path path, Set<String> keywords) {

    }

    private void addExifTags(byte[] source, Path path, Collection<Consumer<TiffOutputDirectory>> dirConsummers) {
        try {
            final TiffOutputSet outputSet;
            ImageMetadata       metadata = Imaging.getMetadata(source);

            // if file does not contain any exif metadata, we create an empty
            // set of exif metadata. Otherwise, we keep all the other
            // existing tags.
            if (metadata == null) {
                outputSet = new TiffOutputSet();
            } else if (metadata instanceof JpegImageMetadata jpegMetadata) {
                final TiffImageMetadata exif = jpegMetadata.getExif();
                if (null != exif) {
                    outputSet = exif.getOutputSet();
                } else {
                    outputSet = new TiffOutputSet();
                }
            } else {
                log.warn("Ignoring ImageMetadata {}.", metadata.getClass().getSimpleName());
                outputSet = new TiffOutputSet();
            }
            try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
                final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
                for (Consumer<TiffOutputDirectory> dirConsummer : dirConsummers) {
                    dirConsummer.accept(exifDirectory);
                }
                new ExifRewriter().updateExifMetadataLossless(source, os, outputSet);
            }
        } catch (IOException | ImageWriteException | ImageReadException e) {
            log.error("File: '{}', Cannot save tag", path, e);
        }
    }
}
