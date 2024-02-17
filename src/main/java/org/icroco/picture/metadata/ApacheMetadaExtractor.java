package org.icroco.picture.metadata;

import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.GenericImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.gif.GifImageMetadataItem;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.icroco.picture.model.Dimension;
import org.jooq.lambda.Unchecked;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ApacheMetadaExtractor implements IMetadataExtractor {

    @Override
    public Map<String, Object> getAllInformation(Path path) {
        try {
            return Imaging.getMetadata(path.toFile())
                          .getItems()
                          .stream()
                          .map(this::toEntry)
                          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (ImageReadException | IOException e) {
            log.error("Cannot parse metadata for: '{}'", path.toAbsolutePath(), e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<MetadataDirectory> getAllByDirectory(Path path) {
        try {

            var metada = Imaging.getMetadata(path.toFile());
            List<Map.Entry<String, Object>> entries = Imaging.getMetadata(path.toFile())
                                                             .getItems()
                                                             .stream()
                                                             .map(this::toEntry)
                                                             .toList();
            Map<String, ?> res = entries.stream()
                                        .collect(Collectors.groupingBy(Map.Entry::getKey,
                                                                       Collectors.mapping(e -> Objects.toString(e.getValue()),
                                                                                          Collectors.joining(","))));
            return List.of(new MetadataDirectory("Default", (Map<String, Object>) res));
        } catch (ImageReadException | IOException e) {
            log.error("Cannot parse metadata for: '{}'", path.toAbsolutePath(), e);
            return Collections.emptyList();
        }
    }

    private Map.Entry<String, Object> toEntry(ImageMetadata.ImageMetadataItem metadata) {
        if (metadata instanceof GenericImageMetadata.GenericImageMetadataItem genericItem) {
            return Map.entry(genericItem.getKeyword(), genericItem.getText());
        } else if (metadata instanceof GifImageMetadataItem gifItem) {
            return Map.entry("Gif", gifItem.toString());
        } else {
            return Map.entry("Unkown", metadata.toString());
        }
    }

    @Override
    public Optional<Integer> orientation(Path path) {
        try {
            ImageMetadata metadata = Imaging.getMetadata(path.toFile());
            if (metadata instanceof JpegImageMetadata jpegMetadate) {
                return Optional.ofNullable(jpegMetadate.findEXIFValue(TiffTagConstants.TIFF_TAG_ORIENTATION))
                               .map(Unchecked.function(TiffField::getIntValue));
            }
        } catch (ImageReadException | IOException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public boolean isFileTypeSupported(Path path) {
        try {
            return ImageFormats.UNKNOWN != Imaging.guessFormat(path.toFile());
        } catch (IOException e) {
            log.error("Cannot detect  image type for: '{}'", path.toAbsolutePath(), e);
            return false;
        }
    }

    @Override
    public Optional<MetadataHeader> header(Path path) {
        try {
            return Optional.ofNullable(Imaging.getImageInfo(path.toFile()))
                           .map(metadata -> {
                               return MetadataHeader.builder()
                                                    .size(new Dimension(metadata.getWidth(), metadata.getHeight()))
                                                    // TODO.
                                                    .build();
                           });
        } catch (ImageReadException | IOException e) {
            log.error("Cannot parse metadata for: '{}'", path.toAbsolutePath(), e);
            return Optional.empty();
        }
    }
}
