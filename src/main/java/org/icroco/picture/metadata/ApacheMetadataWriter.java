package org.icroco.picture.metadata;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegPhotoshopMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.jpeg.iptc.*;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.icroco.picture.util.LangUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    @SneakyThrows
    public void setKeywords(Path path, Set<String> keywords) {
        addIptcTags(Files.readAllBytes(path),
                    path,
                    List.of(recordSet -> updateKeywords(true, recordSet, LangUtils.safeCollection(keywords))));

    }

    @Override
    @SneakyThrows
    public void addKeywords(Path path, Set<String> keywords) {
        addIptcTags(Files.readAllBytes(path),
                    path,
                    List.of(recordSet -> updateKeywords(false, recordSet, LangUtils.safeCollection(keywords))));
    }

    @SneakyThrows
    private void updateKeywords(boolean clearAllBefore, List<IptcRecord> records, Collection<String> keywords) {
        if (clearAllBefore) {
            records.removeIf(iptcRecord -> iptcRecord.iptcType == IptcTypes.KEYWORDS);
        }
        var text = String.join(";", keywords);
        records.add(new IptcRecord(IptcTypes.KEYWORDS, text));
        records.sort(IptcRecord.COMPARATOR);
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

    private void addIptcTags(byte[] source, Path path, Collection<Consumer<List<IptcRecord>>> iptcRecordConsumer) {
        try {
            final PhotoshopApp13Data iptcDate;
            ImageMetadata            metadata   = Imaging.getMetadata(source);
            List<IptcBlock>          newBlocks  = new ArrayList<>();
            List<IptcRecord>         newRecords = new ArrayList<>();
            // if file does not contain any exif metadata, we create an empty
            // set of exif metadata. Otherwise, we keep all the other
            // existing tags.
            if (metadata == null) {
                iptcDate = new PhotoshopApp13Data(newRecords, newBlocks);
            } else if (metadata instanceof JpegImageMetadata jpegMetadata) {
                final JpegPhotoshopMetadata iptc = jpegMetadata.getPhotoshop();
                if (null != iptc) {
                    newBlocks.addAll(iptc.photoshopApp13Data.getRawBlocks());
                    newRecords.addAll(iptc.photoshopApp13Data.getRecords());
                }
            } else {
                log.warn("Ignoring ImageMetadata {}.", metadata.getClass().getSimpleName());
            }
            try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
                for (Consumer<List<IptcRecord>> dirConsummer : iptcRecordConsumer) {
                    dirConsummer.accept(newRecords);
                }
                new JpegIptcRewriter().writeIPTC(source, os, new PhotoshopApp13Data(newRecords, newBlocks));
            }
        } catch (IOException | ImageWriteException | ImageReadException e) {
            log.error("File: '{}', Cannot save tag", path, e);
        }
    }
}
