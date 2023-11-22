package org.icroco.picture.metadata;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.png.PngDirectory;
import lombok.RequiredArgsConstructor;
import org.icroco.picture.config.ImagInConfiguration;
import org.icroco.picture.model.Camera;
import org.icroco.picture.model.Dimension;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.views.util.Collections;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@RequiredArgsConstructor
public class DefaultMetadataExtractor implements IMetadataExtractor {

    public static final  LocalDateTime     EPOCH_0             = Instant.ofEpochMilli(0).atZone(ZoneId.systemDefault()).toLocalDateTime();
    private static final Logger            log                 = org.slf4j.LoggerFactory.getLogger(DefaultMetadataExtractor.class);
    private static final Supplier<Integer> DEFAULT_ORIENTATION = () -> 0;

    private final IKeywordManager tagManager;


    @Override
    @Cacheable(cacheNames = ImagInConfiguration.CACHE_IMAGE_HEADER, unless = "#result != null")
    public Map<String, Object> getAllInformation(Path path) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());

            return Collections.toStream(metadata.getDirectories())
                              .flatMap(d -> d.getTags().stream())
                              .filter(Objects::nonNull)
                              .collect(Collectors.toMap(Tag::getTagName, Tag::getDescription, (o, o2) -> {
                                  log.warn("File: '{}', Duplicate metadata values: {} <-> {}", path, o, o2);
                                  return o2;
                              }));
        } catch (Exception e) {
            log.error("Cannot read metadata for: {}", path, e);
        }

        return java.util.Collections.emptyMap();
    }

    @Override
    public Optional<Integer> orientation(InputStream input) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(input, input.available());
            return readdAllHeaders(metadata,
                                   ExifIFD0Directory.class,
                                   ExifIFD0Directory.TAG_ORIENTATION,
                                   d -> getTagAsInt(Optional.of(d),
                                                    ExifDirectoryBase.TAG_ORIENTATION,
                                                    DEFAULT_ORIENTATION,
                                                    t -> log.warn("Cannot read orientation")));
        } catch (Throwable ex) {
            log.warn("Cannot read orientation, message: {}", ex.getLocalizedMessage());
        }
        return Optional.empty();
//        return header(Path.of(""), input).map(MetadataHeader::orientation);
    }

    @Override
    public Optional<MetadataHeader> header(final Path path, final InputStream input) {
        try {

            Metadata metadata = ImageMetadataReader.readMetadata(input, input.available());

//            var tags = StreamSupport.stream(metadata.getDirectories().spliterator(), false)
//                    .flatMap(directory -> directory.getTags().stream())
//                    .collect(Collectors.toMap(Tag::getTagType, tag -> tag));
//            for (Directory directory : metadata.getDirectories()) {
//                System.out.println("Directory: " + directory);
//                for (Tag tag : directory.getTags()) {
//                    System.out.printf("   Tag: %s (%d/%s): %s%n", tag.getTagName(), tag.getTagType(), tag.getTagTypeHex(), tag.getDescription());
//                }
//            }
            var edb                    = ofNullable(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class));
            var firstExifIFD0Directory = ofNullable(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class));
            return Optional.of(MetadataHeader.builder()
                                             .orginalDate(originalDateTime(path, metadata).orElse(EPOCH_0))
                                             .orientation(extractOrientation(path, firstExifIFD0Directory))
                                             .geoLocation(gps(path,
                                                              metadata).map(gl -> new org.icroco.picture.model.GeoLocation(gl.getLatitude(),
                                                                                                                           gl.getLongitude()))
                                                                       .orElse(NO_WHERE))
                                             .size(extractSize(metadata, path, edb))
                                             .camera(extractCamera(path, firstExifIFD0Directory))
                                             .keywords(extractKeywords(path,
                                                                       ofNullable(metadata.getFirstDirectoryOfType(IptcDirectory.class))))
                                             .build());
        } catch (Throwable ex) {
            log.warn("Cannot read header for file: '{}', message: {}", path, ex.getLocalizedMessage());
            return Optional.empty();
        }
    }

    private Camera extractCamera(Path path, Optional<ExifIFD0Directory> firstExifIFD0Directory) {
        return firstExifIFD0Directory.map(d -> new Camera(getTagAsString(firstExifIFD0Directory,
                                                                         ExifDirectoryBase.TAG_MAKE,
                                                                         () -> "_",
                                                                         t -> log.warn("'{}' Cannot read camera make", path)),
                                                          getTagAsString(firstExifIFD0Directory,
                                                                         ExifDirectoryBase.TAG_MODEL,
                                                                         () -> "_",
                                                                         t -> log.warn("'{}' Cannot read camera model", path))))
                                     .orElse(Camera.UNKWOWN_CAMERA);
    }


    public Set<Keyword> extractKeywords(Path path, Optional<IptcDirectory> firstIptcDirectory) {
        return firstIptcDirectory.map(directory -> {
                                     var keywords = directory.getStringArray(IptcDirectory.TAG_KEYWORDS);

                                     if (keywords == null || keywords.length == 0) {
                                         return java.util.Collections.<Keyword>emptySet();
                                     }

                                     return Arrays.stream(keywords)
                                                  .map(tagManager::findOrCreateTag)
                                                  .collect(Collectors.toSet());
                                 })
                                 .orElse(java.util.Collections.emptySet());
    }


    private static <T extends Directory, R> Optional<R> readdAllHeaders(Metadata metadata,
                                                                        Class<T> type,
                                                                        int tagId,
                                                                        Function<T, R> extrator) {
        return StreamSupport.stream(metadata.getDirectories().spliterator(), false)
                            .filter(type::isInstance)
                            .map(type::cast)
                            .findFirst()
                            .map(extrator);
    }

    private Optional<GeoLocation> gps(Path path, Metadata metadata) {
        return ofNullable(metadata.getFirstDirectoryOfType(GpsDirectory.class))
                .map(GpsDirectory::getGeoLocation);

    }

//
//    public Optional<byte[]> thumbnail(Path path, Metadata metadata) {
//        return Optional.ofNullable(metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class))
//                       .map(Unchecked.function(this::getThumbnail, throwable -> log.warn("'{}' Cannot read orientation", path)));
//    }


    public Dimension extractSize(Metadata metadata, Path path, Optional<? extends ExifDirectoryBase> directory) {
        return directory.map(_ -> new Dimension(getTagAsInt(directory,
                                                            ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH,
                                                            () -> 0,
                                                            t -> log.warn("'{}' Cannot read width", path)),
                                                getTagAsInt(directory,
                                                            ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT,
                                                            () -> 0,
                                                            t -> log.warn("'{}' Cannot read height", path))))
                        .orElseGet(() -> extractPngSize(metadata, path, ofNullable(metadata.getFirstDirectoryOfType(PngDirectory.class))));
    }

    public Dimension extractPngSize(Metadata metadata, Path path, Optional<? extends PngDirectory> directory) {
        return directory.map(_ -> new Dimension(getTagAsInt(directory,
                                                            PngDirectory.TAG_IMAGE_WIDTH,
                                                            () -> 0,
                                                            t -> log.warn("'{}' Cannot read width", path)),
                                                getTagAsInt(directory,
                                                            PngDirectory.TAG_IMAGE_HEIGHT,
                                                            () -> 0,
                                                            t -> log.warn("'{}' Cannot read height", path))))
                        .orElse(Dimension.EMPTY_DIM);
    }

    public short extractOrientation(Path path, Optional<? extends ExifDirectoryBase> exif) {
        return (short) getTagAsInt(exif, ExifDirectoryBase.TAG_ORIENTATION,
                                   DEFAULT_ORIENTATION, t -> log.warn("'{}' Cannot read orientation", path));

//        return exif.map(Unchecked.function(this::getOrientation, throwable -> log.warn("'{}' Cannot read orientation", path)));
    }

    public Optional<LocalDateTime> originalDateTime(Path path, Metadata metadata) {
        return ofNullable(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class))
                .map(Unchecked.function(this::getDateTime, throwable -> log.warn("'{}' Cannot read original date time", path)));
    }

//    Integer getOrientation(Directory directory) {
//        return getTagAsInt(directory, ExifDirectoryBase.TAG_ORIENTATION, () -> 1, t -> log.warn("'{}' Cannot read orientation", path));
////
////        if (directory.containsTag(ExifDirectoryBase.TAG_ORIENTATION)) {
////            return directory.getInt(ExifDirectoryBase.TAG_ORIENTATION);
////        }
////        return 1;
//    }

    static <T extends Directory> int getTagAsInt(Optional<T> directory,
                                                 int tagId,
                                                 Supplier<Integer> defaultValueSupplier,
                                                 Consumer<Throwable> errorConsummer) {
        return directory.filter(d -> d.containsTag(tagId))
                        .map(d -> Unchecked.function(d::getInt, errorConsummer).apply(tagId))
                        .orElseGet(defaultValueSupplier);
    }

    static <T extends Directory> String getTagAsString(Optional<T> directory,
                                                       int tagId,
                                                       Supplier<String> defaultValueSupplier,
                                                       Consumer<Throwable> errorConsummer) {
        return directory.filter(d -> d.containsTag(tagId))
                        .map(d -> Unchecked.<Integer, String>function(d::getString, errorConsummer).apply(tagId))
                        .orElseGet(defaultValueSupplier);
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
