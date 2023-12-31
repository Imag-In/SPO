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
import com.drew.metadata.icc.IccDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
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

    public static final LocalDateTime EPOCH_0 = LocalDateTime.of(0, 1, 1, 0, 0);
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
                              .filter(tag -> tag.getDescription() != null)
                              .filter(tag -> tag.getTagName() != null)
                              .collect(Collectors.toMap(Tag::getTagName, Tag::getDescription, (o, o2) -> {
                                  log.warn("File: '{}', Duplicate metadata values: {} <-> {}", path, o, o2);
                                  return o2;
                              }));
        } catch (Exception e) {
            log.error("Cannot read metadata for: {}", path, e);
        }

        return java.util.Collections.emptyMap();
    }

    public static void printInformation(Path path) {
        Unchecked.runnable(() -> {
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
            Collections.toStream(metadata.getDirectories())
                       .forEach(d -> {
                           log.info("Directory: {}", d.getClass());
                           d.getTags()
                            .forEach(t -> {
                                log.info("   tags: {}({}) = {}", t.getTagName(), t.getTagType(), t.getDescription());
                            });
                       });
        }).run();
    }

    @Override
    public Optional<Integer> orientation(InputStream input) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(input, input.available());
            return readdAllHeaders(metadata,
                                   ExifIFD0Directory.class,
                                   d -> getTagAsInt(Optional.of(d),
                                                    ExifDirectoryBase.TAG_ORIENTATION,
                                                    DEFAULT_ORIENTATION,
                                                    _ -> log.warn("Cannot read orientation")));
        } catch (Throwable ex) {
            log.warn("Cannot read orientation, message: {}", ex.getLocalizedMessage());
        }
        return Optional.empty();
//        return header(Path.of(""), input).map(MetadataHeader::orientation);
    }

    @Override
    public Optional<MetadataHeader> header(final Path path, final InputStream input) {
        try {

            Metadata metadata               = ImageMetadataReader.readMetadata(input, input.available());
            var      edb                    = ofNullable(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class));
            var      firstExifIFD0Directory = ofNullable(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class));
            var gps = gps(path, metadata)
                    .map(gl -> new org.icroco.picture.model.GeoLocation(gl.getLatitude(),
                                                                        gl.getLongitude()))
                    .orElse(NO_WHERE);
            return Optional.of(MetadataHeader.builder()
                                             .orginalDate(originalDateTime(path, metadata).orElse(EPOCH_0))
                                             .orientation(extractOrientation(path, firstExifIFD0Directory))
                                             .geoLocation(gps(path, metadata)
                                                                  .map(gl -> new org.icroco.picture.model.GeoLocation(gl.getLatitude(),
                                                                                                                      gl.getLongitude()))
                                                                  .orElse(NO_WHERE))
                                             .size(extractSize(metadata, path, edb, firstExifIFD0Directory))
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
                                                                         _ -> log.warn("'{}' Cannot read camera make", path)),
                                                          getTagAsString(firstExifIFD0Directory,
                                                                         ExifDirectoryBase.TAG_MODEL,
                                                                         () -> "_",
                                                                         _ -> log.warn("'{}' Cannot read camera model", path))))
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
                                                                        Function<T, R> extractor) {
        return StreamSupport.stream(metadata.getDirectories().spliterator(), false)
                            .filter(type::isInstance)
                            .map(type::cast)
                            .findFirst()
                            .map(extractor);
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


    public Dimension extractSize(Metadata metadata, Path path, Optional<ExifSubIFDDirectory> directory,
                                 Optional<ExifIFD0Directory> firstExifIFD0Directory) {
        return extractSizeDD(path, directory)
                .or(() -> extractSizeD0(path, firstExifIFD0Directory))
                .or(() -> extractPngSize(metadata, path, ofNullable(metadata.getFirstDirectoryOfType(PngDirectory.class))))
                .or(() -> extractJpgSize(metadata, path, ofNullable(metadata.getFirstDirectoryOfType(JpegDirectory.class))))
                .orElse(Dimension.EMPTY_DIM);
    }

    Optional<Dimension> extractSizeDD(Path path, Optional<ExifSubIFDDirectory> directory) {
        Optional<Integer> width = getTagAsInt(directory,
                                              ExifSubIFDDirectory.TAG_IMAGE_WIDTH,
                                              t -> log.warn("'{}' Cannot read width into ExifSubIFDDirectory", path));
        Optional<Integer> height = getTagAsInt(directory,
                                               ExifSubIFDDirectory.TAG_IMAGE_WIDTH,
                                               t -> log.warn("'{}' Cannot read height into ExifSubIFDDirectory", path));
        if (height.isPresent() && width.isPresent()) {
            return Optional.of(new Dimension(width.get(), height.get()));
        }
        return Optional.empty();
    }

    Optional<Dimension> extractSizeD0(Path path, Optional<ExifIFD0Directory> directory) {
        Optional<Integer> width = getTagAsInt(directory,
                                              ExifIFD0Directory.TAG_IMAGE_WIDTH,
                                              t -> log.warn("'{}' Cannot read width into ExifIFD0Directory", path));
        Optional<Integer> height = getTagAsInt(directory,
                                               ExifIFD0Directory.TAG_IMAGE_HEIGHT,
                                               t -> log.warn("'{}' Cannot read height into ExifIFD0Directory", path));
        if (height.isPresent() && width.isPresent()) {
            return Optional.of(new Dimension(width.get(), height.get()));
        }
        return Optional.empty();
    }

    public Optional<Dimension> extractPngSize(Metadata metadata, Path path, Optional<? extends PngDirectory> directory) {
        Optional<Integer> width = getTagAsInt(directory,
                                              PngDirectory.TAG_IMAGE_WIDTH,
                                              t -> log.warn("'{}' Cannot read width into PngDirectory", path));
        Optional<Integer> height = getTagAsInt(directory,
                                               PngDirectory.TAG_IMAGE_HEIGHT,
                                               t -> log.warn("'{}' Cannot read height into PngDirectory", path));
        if (height.isPresent() && width.isPresent()) {
            return Optional.of(new Dimension(width.get(), height.get()));
        }
        return Optional.empty();
    }


    public Optional<Dimension> extractJpgSize(Metadata metadata, Path path, Optional<? extends JpegDirectory> directory) {
        Optional<Integer> width = getTagAsInt(directory,
                                              JpegDirectory.TAG_IMAGE_WIDTH,
                                              t -> log.warn("'{}' Cannot read width into JpegDirectory", path));
        Optional<Integer> height = getTagAsInt(directory,
                                               JpegDirectory.TAG_IMAGE_HEIGHT,
                                               t -> log.warn("'{}' Cannot read height into JpegDirectory", path));
        if (height.isPresent() && width.isPresent()) {
            return Optional.of(new Dimension(width.get(), height.get()));
        }
        return Optional.empty();
    }

    public short extractOrientation(Path path, Optional<? extends ExifDirectoryBase> exif) {
        return (short) getTagAsInt(exif, ExifDirectoryBase.TAG_ORIENTATION,
                                   DEFAULT_ORIENTATION, t -> log.warn("'{}' Cannot read orientation", path));

//        return exif.map(Unchecked.function(this::getOrientation, throwable -> log.warn("'{}' Cannot read orientation", path)));
    }

    public Optional<LocalDateTime> originalDateTime(Path path, Metadata metadata) {
        // First look at TAG_DATETIME_ORIGINAL
        return getTagAsDate(ofNullable(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class)),
                            ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL,
                            Optional.of(ExifSubIFDDirectory.TAG_TIME_ZONE_ORIGINAL),
                            _ -> log.warn("'{}' Cannot read DATETIME_ORIGINAL into ExifSubIFDDirectory", path))

                .or(() -> getTagAsDate(ofNullable(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class)),
                                       ExifIFD0Directory.TAG_DATETIME_ORIGINAL,
                                       Optional.of(ExifIFD0Directory.TAG_TIME_ZONE_ORIGINAL),
                                       _ -> log.warn("'{}' Cannot read DATETIME_ORIGINAL into ExifIFD0Directory", path)))
                // Then Look at TAG_DATETIME_DIGITIZED
                .or(() -> getTagAsDate(ofNullable(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class)),
                                       ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED,
                                       Optional.of(ExifSubIFDDirectory.TAG_TIME_ZONE_DIGITIZED),
                                       _ -> log.warn("'{}' Cannot read TAG_TIME_ZONE_DIGITIZED into ExifSubIFDDirectory", path)))
                .or(() -> getTagAsDate(ofNullable(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class)),
                                       ExifIFD0Directory.TAG_DATETIME_DIGITIZED,
                                       Optional.of(ExifIFD0Directory.TAG_TIME_ZONE_DIGITIZED),
                                       _ -> log.warn("'{}' Cannot read TAG_DATETIME_DIGITIZED into ExifIFD0Directory", path)))
                .or(() -> getTagAsDate(ofNullable(metadata.getFirstDirectoryOfType(IccDirectory.class)),
                                       IccDirectory.TAG_PROFILE_DATETIME,
                                       Optional.empty(),
                                       _ -> log.warn("'{}' Cannot read TAG_DATETIME_DIGITIZED into ExifIFD0Directory", path)))
                .or(() -> getTagAsDate(ofNullable(metadata.getFirstDirectoryOfType(IptcDirectory.class)),
                                       IptcDirectory.TAG_DATE_CREATED,
                                       Optional.empty(),
                                       _ -> log.warn("'{}' Cannot read TAG_DATETIME_DIGITIZED into ExifIFD0Directory", path)))
                .or(() -> getTagAsDate(ofNullable(metadata.getFirstDirectoryOfType(GpsDirectory.class)),
                                       GpsDirectory.TAG_DATE_STAMP,
                                       Optional.empty(),
                                       _ -> log.warn("'{}' Cannot read TAG_DATETIME_DIGITIZED into ExifIFD0Directory", path)))
                ;

//        return ofNullable(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class))
//                .map(Unchecked.function(this::getDateTime, _ -> log.warn("'{}' Cannot read original date time", path)));
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

    static <T extends Directory> Optional<Integer> getTagAsInt(Optional<T> directory,
                                                               int tagId,
                                                               Consumer<Throwable> errorConsummer) {
        return directory.filter(d -> d.containsTag(tagId))
                        .map(d -> Unchecked.function(d::getInt, errorConsummer).apply(tagId));
    }

    LocalDateTime getDateTime(ExifSubIFDDirectory directory) {
        if (directory.containsTag(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)) {
            return LocalDateTime.ofInstant(directory.getDateOriginal(TimeZone.getDefault()).toInstant(), ZoneId.systemDefault());
        }
        return EPOCH_0;
    }

    static <T extends Directory> Optional<LocalDateTime> getTagAsDate(Optional<T> directory,
                                                                      int tagId,
                                                                      Optional<Integer> tagZoneId,
                                                                      Consumer<Throwable> errorConsummer) {
        ZoneId zId = tagZoneId.flatMap(zoneId -> directory.filter(d -> d.containsTag(zoneId))
                                                          .map(d -> d.getString(zoneId)))
                              .map(ZoneId::of)
                              .orElse(null);

        Optional<LocalDateTime> optDate = directory.filter(d -> d.containsTag(tagId))
                                                   .map(d -> Unchecked.function((Directory dir) -> dir.getDate(tagId), errorConsummer)
                                                                      .apply(d))
                                                   .map(date -> LocalDateTime.ofInstant(date.toInstant(),
                                                                                        zId == null ? ZoneId.systemDefault() : zId));

        return optDate;
    }

    static <T extends Directory> String getTagAsString(Optional<T> directory,
                                                       int tagId,
                                                       Supplier<String> defaultValueSupplier,
                                                       Consumer<Throwable> errorConsummer) {
        return directory.filter(d -> d.containsTag(tagId))
                        .map(d -> Unchecked.<Integer, String>function(d::getString, errorConsummer).apply(tagId))
                        .orElseGet(defaultValueSupplier);
    }


//    byte[] getThumbnail(ExifThumbnailDirectory directory) {
//        return directory.getObject(TAG_THUMBNAIL_DATA)
//    }
}
