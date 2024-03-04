package org.icroco.picture.metadata;

import io.jbock.util.Either;
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
import org.apache.commons.imaging.formats.jpeg.xmp.JpegXmpRewriter;
import org.apache.commons.imaging.formats.tiff.JpegImageData;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.io.FilenameUtils;
import org.apache.xmlgraphics.xmp.Metadata;
import org.apache.xmlgraphics.xmp.XMPParser;
import org.apache.xmlgraphics.xmp.XMPSerializer;
import org.icroco.picture.metadata.xmp.XMPBasicSchema;
import org.icroco.picture.model.ERating;
import org.icroco.picture.model.ERotation;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.util.LangUtils;
import org.icroco.picture.util.SpoException;
import org.jooq.lambda.function.Consumer3;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ApacheMetadataWriter implements IMetadataWriter {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss", Locale.US);

    @Override
    @SneakyThrows
    public Either<Exception, Path> setOrignialDate(Path path, LocalDateTime date) {
        try {
            // WARN: Could use a lot of memory, but mandaroty with Imaging, otherwize we need to copy the file.
            final byte[] source = Files.readAllBytes(path);
            addExifTags(source, path, List.of((_, exif, _) -> updateOriginalDate(exif, date)));

            return Either.right(path);
        } catch (Exception e) {
            return Either.left(e);
        }
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
    public Either<Exception, Path> setOrientation(Path path, ERotation orientation) {
        try {
            // WARN: Could use a lot of memory, but mandaroty with Imaging, otherwize we need to copy the file.
            final byte[] source = Files.readAllBytes(path);
            addExifTags(source, path, List.of((root, _, _) -> updateOrientation(root, orientation.getOrientation())));

            return Either.right(path);
        } catch (Exception e) {
            return Either.left(e);
        }
    }

    @SneakyThrows
    private void updateOrientation(TiffOutputDirectory dir, int orientation) {
        dir.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
        dir.add(TiffTagConstants.TIFF_TAG_ORIENTATION, (short) orientation);
    }

    @Override
    @SneakyThrows
    public void setKeywords(Path path, Set<Keyword> keywords) {
        addIptcTags(Files.readAllBytes(path),
                    path,
                    List.of(recordSet -> updateKeywords(true, recordSet, LangUtils.safeCollection(keywords))));

    }

    @Override
    @SneakyThrows
    public void addKeywords(Path path, Set<Keyword> keywords) {
        addIptcTags(Files.readAllBytes(path),
                    path,
                    List.of(recordSet -> updateKeywords(false, recordSet, LangUtils.safeCollection(keywords))));
    }

    @SneakyThrows
    private void updateKeywords(boolean clearAllBefore, List<IptcRecord> records, Collection<Keyword> keywords) {
        if (clearAllBefore) {
            records.removeIf(iptcRecord -> iptcRecord.iptcType == IptcTypes.KEYWORDS);
        }
        var text = keywords.stream().map(Keyword::name).collect(Collectors.joining(";"));
        records.add(new IptcRecord(IptcTypes.KEYWORDS, text));
        records.sort(IptcRecord.COMPARATOR);
    }

    @Override
    public void removeKeywords(Path path, Set<String> keywords) {
        // TODO:
        log.error("Not Yet Implemented");
    }

    @Override
    public Either<Exception, Path> setRating(Path path, ERating rating) {
        try {
            addXmpTags(Files.readAllBytes(path),
                       path,
                       List.of(xmpMetaData -> updateRating(false, xmpMetaData, rating)));
            return Either.right(path);
        } catch (Exception e) {
            return Either.left(e);
        }
    }

    @SneakyThrows
    private void updateRating(boolean clearAllBefore, Metadata xmpMetaData, ERating rating) {
        final XMPBasicSchema.XMPBasicAdapter xmp = XMPBasicSchema.getAdapter(xmpMetaData);
        xmp.setRating(rating.getCode());
    }

    @Override
    public Either<Exception, Path> setThumbnail(Path path, byte[] thumbnail) {
        try {
            var current = getImageAndMetadata(path);

            // Get the writer
            String                format  = FilenameUtils.getExtension(path.toString());
            Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(format);
            ImageWriter           writer  = null;
            while (writers.hasNext()) {
                var next = writers.next();
                if (next != null) {
                    writer = next;
                    break;
                }
            }
            if (writer == null) {
                throw new IllegalArgumentException(STR."No writer for: \{format}");
            }

            // Create output stream (in try-with-resource block to avoid leaks)
            try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
                writer.setOutput(output);
                ImageWriteParam param = writer.getDefaultWriteParam();
//                param.setCompressionMode(ImageWriteParam.MODE_DISABLED);
//                param.setTilingMode(ImageWriteParam.MODE_DISABLED);
//                param.setCompressionMode(ImageWriteParam.MODE_DISABLED);
                writer.write(null,
                             new IIOImage(current.image, List.of(ImageIO.read(new ByteArrayInputStream(thumbnail))), current.metadata),
                             param);
            } finally {
                writer.dispose();
            }
            return Either.right(path);
        } catch (Exception e) {
            return Either.left(e);
        }
    }

    record ImageAndMetadata(BufferedImage image, IIOMetadata metadata) {
    }

    ImageAndMetadata getImageAndMetadata(Path path) {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            // Get the reader
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input);
                    ImageReadParam param = reader.getDefaultReadParam();
                    return new ImageAndMetadata(reader.read(0, param), reader.getImageMetadata(0));
                } finally {
                    reader.dispose();
                }
            }
        } catch (Throwable e) {
            log.error("Cannot extract thumbnail from: '{}', message: {}", path, e.getLocalizedMessage());
        }
        return null;
    }

    @SneakyThrows
    private void updateThumbnail(TiffOutputDirectory dir, byte[] thumbnail) {
//        dir.setJpegImageData();
        dir.setJpegImageData(new JpegImageData(-1, thumbnail.length, thumbnail));
    }

    private void addExifTags(byte[] source,
                             Path path,
                             Collection<Consumer3<TiffOutputDirectory, TiffOutputDirectory, TiffOutputDirectory>> dirConsummers) {
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
                var rootDirectory = outputSet.getOrCreateRootDirectory();
                var exifDirectory = outputSet.getOrCreateExifDirectory();
                var gpsDirectory  = outputSet.getOrCreateGPSDirectory();
                for (Consumer3<TiffOutputDirectory, TiffOutputDirectory, TiffOutputDirectory> dirConsummer : dirConsummers) {
                    dirConsummer.accept(rootDirectory, exifDirectory, gpsDirectory);
                }
                new ExifRewriter().updateExifMetadataLossless(source, os, outputSet);
                os.flush();
            }
        } catch (IOException | ImageWriteException | ImageReadException e) {
            log.error("File: '{}', Cannot save tag", path, e);
        }
    }

    private void addXmpTags(byte[] source,
                            Path path,
                            Collection<Consumer<Metadata>> dirConsummers) {
        try {

            final String   xmpXml = getXmpXml(source);
            final Metadata xmpMetaData;

            if (xmpXml == null) {
                xmpMetaData = new Metadata();
            } else {
                final ByteArrayInputStream input = new ByteArrayInputStream(xmpXml.getBytes(StandardCharsets.UTF_8));
                xmpMetaData = XMPParser.parseXMP(new StreamSource(input));
            }

            for (Consumer<Metadata> dirConsummer : dirConsummers) {
                dirConsummer.accept(xmpMetaData);
            }

            final String xmpAsString;
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                XMPSerializer.writeXMPPacket(xmpMetaData, bos, false);
                xmpAsString = bos.toString(StandardCharsets.UTF_8);
            }

            try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
                new JpegXmpRewriter().updateXmpXml(source, os, xmpAsString);
                os.flush();
            }
        } catch (IOException | ImageWriteException | ImageReadException | TransformerException | SAXException e) {
            log.error("File: '{}', Cannot save tag", path, e);
        }
    }

    private String getXmpXml(byte[] imageBytes) {
        final String xmpString;
        try {
            xmpString = Imaging.getXmpXml(imageBytes);
        } catch (ImageReadException ex) {
            throw new SpoException("Unable to parse the image.", ex);
        } catch (IOException ex) {
            throw new SpoException("Unable to read the image data.", ex);
        }

//        return CommonUtils.formatXml(xmpString, false);
        return xmpString;
    }

    private void addExifRootTags(byte[] source, Path path, Collection<Consumer<TiffOutputDirectory>> dirConsummers) {
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
                os.flush();
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
            if (metadata instanceof JpegImageMetadata jpegMetadata) {
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
                os.flush();
            }
        } catch (IOException | ImageWriteException | ImageReadException e) {
            log.error("File: '{}', Cannot save tag", path, e);
        }
    }
}
