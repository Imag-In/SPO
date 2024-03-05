package org.icroco.picture.model;

import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.util.Callback;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.icroco.picture.util.SpoException;
import org.icroco.picture.views.util.FxPlatformExecutor;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Data
@ToString(exclude = { "originalDateProperty", "keepOrThrowProperty" })
@EqualsAndHashCode(of = { "fullPath" })
public class MediaFile implements IMediaFile {
    public static  Comparator<MediaFile> HASH_COMPARATOR = Comparator.comparing(MediaFile::getHash);
    private static LocalDateTime         TIMESTAMP       = LocalDateTime.MIN.plusHours(1L);

    private Long           id;
    private Path           fullPath;
    private String         fileName;
    private LocalDateTime originalDate;
    private Set<Keyword>   keywords;
    private GeoLocation    geoLocation;
    private String         hash;
    private LocalDate      hashDate;
    private Dimension      dimension;
    private Short          orientation;
    private Camera         camera;
    private Integer        collectionId;
    private boolean        selected;
    private EKeepOrThrow  keepOrThrow;
    private EThumbnailType thumbnailType;
    private ERating       rating;
    private UUID          reference;

    private final SimpleObjectProperty<LocalDateTime> lastUpdatedProperty   = new SimpleObjectProperty<>(LocalDateTime.MIN);
    private final SimpleBooleanProperty               loadedInCacheProperty = new SimpleBooleanProperty(false);
    private final ObjectProperty<LocalDateTime>       originalDateProperty;
    private final ObjectProperty<EKeepOrThrow>        keepOrThrowProperty;

    @SuppressWarnings("unchecked")
    @Builder
    public MediaFile(Long id,
                     Path fullPath,
                     String fileName,
                     LocalDateTime originalDate,
                     Set<Keyword> keywords,
                     GeoLocation geoLocation,
                     String hash,
                     LocalDate hashDate,
                     Dimension dimension,
                     Short orientation,
                     Camera camera,
                     Integer collectionId,
                     boolean selected,
                     EKeepOrThrow keepOrThrow,
                     EThumbnailType thumbnailType,
                     ERating rating,
                     UUID reference) {
        this.id = id;
        this.fullPath = fullPath;
        this.fileName = fileName;
        this.originalDate = originalDate;
        this.keywords = keywords;
        this.geoLocation = geoLocation;
        this.hash = hash;
        this.hashDate = hashDate;
        this.dimension = dimension;
        this.orientation = orientation;
        this.camera = camera;
        this.collectionId = collectionId;
        this.selected = selected;
        this.rating = rating;
        this.reference = reference;
        this.thumbnailType = Objects.requireNonNullElse(thumbnailType, EThumbnailType.ABSENT);
        this.keepOrThrow = Objects.requireNonNullElse(keepOrThrow, EKeepOrThrow.UNKNOW);

        // TODO: use JavaBeanObjectPropertyBuilder.create() ?
        try {
            this.originalDateProperty = (ObjectProperty<LocalDateTime>) JavaBeanObjectPropertyBuilder.create()
                                                                                                     .bean(this)
                                                                                                     .name("originalDate")
                                                                                                     .build();
            this.keepOrThrowProperty = (ObjectProperty<EKeepOrThrow>) JavaBeanObjectPropertyBuilder.create()
                                                                                                   .bean(this)
                                                                                                   .name("keepOrThrow")
                                                                                                   .build();
        } catch (NoSuchMethodException e) {
            throw new SpoException("Technical Error", e);
        }
    }

    public static Callback<MediaFile, Observable[]> extractor() {
        return mf -> new Observable[] { mf.loadedInCacheProperty, mf.lastUpdatedProperty, mf.originalDateProperty };
    }

    @Override
    public long id() {
        return getId();
    }

    @Override
    public Path fullPath() {
        return getFullPath();
    }

    @Override
    public String fileName() {
        return getFileName();
    }

    @Override
    public LocalDateTime originalDate() {
        return getOriginalDate();
    }

    @Override
    public Set<Keyword> tags() {
        return getKeywords();
    }

    @Override
    public GeoLocation geoLocation() {
        return getGeoLocation();
    }

    @Override
    public Dimension dimension() {
        return getDimension();
    }

    public Set<Keyword> getKeywords() {
        return keywords == null ? Collections.emptySet() : keywords;
    }

    @Override
    public Short orientation() {
        return orientation;
    }

    @Override
    public Camera camera() {
        return getCamera();
    }

    public void invertSelection() {
        selected = !selected;
    }

    public void setLoadedInCacheProperty(boolean value) {
        loadedInCacheProperty.set(value);
    }

    public boolean isLoadedInCache() {
        return loadedInCacheProperty.get();
    }

    public void setLastUpdated(LocalDateTime time) {
        lastUpdatedProperty.set(time);
    }

    public void initFrom(MediaFile source) {
        if (source.id != null) {
            this.id = source.id;
        }
        this.fullPath = source.fullPath;
        this.fileName = source.fileName;
        this.keywords = source.keywords;
        this.originalDate = source.originalDate;
        this.keepOrThrow = source.keepOrThrow;
        this.geoLocation = source.geoLocation;
        this.hash = source.hash;
        this.hashDate = source.hashDate;
        this.dimension = source.dimension;
        this.orientation = source.orientation;
        this.camera = source.camera;
        this.collectionId = source.collectionId;
        this.selected = source.selected;
        this.thumbnailType = source.thumbnailType;
        this.rating = source.rating;
        this.reference = source.reference;
    }

    public void resetProperties() {
        FxPlatformExecutor.fxRun(() -> {
            loadedInCacheProperty.setValue(false);
            keepOrThrowProperty.setValue(keepOrThrow);
            originalDateProperty.setValue(originalDate);
            lastUpdatedProperty.setValue(LocalDateTime.now());
        });
    }

    public void setNextKeepOrThrow() {
        keepOrThrowProperty.setValue(keepOrThrowProperty.get().next());
    }
}

//public record MediaFile(long mcId,
//                        Path fullPath,
//                        String fileName,
//                        LocalDate originalDate,
//                        Set<Tag> tags,
//                        SimpleObjectProperty<Thumbnail> thumbnail) implements IMediaFile {
//
//    @Builder
//    public MediaFile(long mcId, Path fullPath, String fileName, LocalDate originalDate, Set<Tag> tags, SimpleObjectProperty<Thumbnail> thumbnail) {
//        this.mcId = mcId;
//        this.fullPath = fullPath;
//        this.fileName = fileName;
//        this.originalDate = originalDate;
//        this.tags = tags;
//        this.thumbnail = thumbnail == null ? new SimpleObjectProperty<>(null) : thumbnail;
//    }
//
////    public EThumbnailType status() {
////        return thumbnail.getOrigin().get();
////    }
////
////    public Image getThumbnail() {
////        return thumbnail.getThumbnail();
////    }
//
//    @Override
//    public int hashCode() {
//        return fullPath.hashCode();
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        MediaFile mediaFile = (MediaFile) o;
//        return mcId == mediaFile.mcId;
//    }
//
//    public static Callback<MediaFile, Observable[]> extractor() {
//        return p -> new Observable[]{ p.thumbnail };
//    }
//}
