package org.icroco.picture.model;

import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Callback;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

@Data
@EqualsAndHashCode(of = { "fullPath" })
public class MediaFile implements IMediaFile {
    public static  Comparator<MediaFile> HASH_COMPARATOR = Comparator.comparing(MediaFile::getHash);
    private static LocalDateTime         TIMESTAMP       = LocalDateTime.MIN.plusHours(1L);

    private Long           id;
    private Path           fullPath;
    private String         fileName;
    private Set<Keyword>   keywords;
    private GeoLocation    geoLocation;
    private String         hash;
    private LocalDate      hashDate;
    private Dimension      dimension;
    private Short          orientation;
    private Camera         camera;
    private Integer        collectionId;
    private boolean        selected;
    private EThumbnailType thumbnailType;

    private final SimpleObjectProperty<LocalDateTime> lastUpdated          = new SimpleObjectProperty<>(LocalDateTime.MIN);
    private final SimpleBooleanProperty              loadedInCache       = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<LocalDateTime> originalDateProperty = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<EKeepOrThrow> keepOrThrowProperty = new SimpleObjectProperty<>();

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
                     EThumbnailType thumbnailType) {
        this.id = id;
        this.fullPath = fullPath;
        this.fileName = fileName;
        this.keywords = keywords;
        this.geoLocation = geoLocation;
        this.hash = hash;
        this.hashDate = hashDate;
        this.dimension = dimension;
        this.orientation = orientation;
        this.camera = camera;
        this.collectionId = collectionId;
        this.selected = selected;

        this.thumbnailType = Objects.requireNonNullElse(thumbnailType, EThumbnailType.ABSENT);
        this.originalDateProperty.setValue(originalDate);
        this.keepOrThrowProperty.setValue(Objects.requireNonNullElse(keepOrThrow, EKeepOrThrow.UNKNOW));
    }

    public static Callback<MediaFile, Observable[]> extractor() {
        return mf -> new Observable[] { mf.loadedInCache, mf.lastUpdated, mf.originalDateProperty };
    }


    public SimpleBooleanProperty loadedInCacheProperty() {
        return loadedInCache;
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
        return originalDateProperty.getValue();
    }

    public LocalDateTime getOriginalDate() {
        return originalDateProperty.getValue();
    }

    public ReadOnlyObjectProperty<LocalDateTime> getOriginalDateProperty() {
        return originalDateProperty;
    }

    public void setOriginalDate(LocalDateTime originalDate) {
        originalDateProperty.setValue(originalDate);
    }

    public ReadOnlyObjectProperty<EKeepOrThrow> getKeepOrThrowProperty() {
        return keepOrThrowProperty;
    }

    public void setKeepOrThrow(EKeepOrThrow keepOrThrow) {
        keepOrThrowProperty.setValue(keepOrThrow);
    }

    public EKeepOrThrow getKeepOrThrow() {
        return keepOrThrowProperty.get();
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

    public void setLoadedInCache(boolean value) {
        loadedInCache.set(value);
    }

    public boolean isLoadedInCache() {
        return loadedInCache.get();
    }

    public void setLastUpdated(LocalDateTime time) {
        lastUpdated.set(time);
    }

    public void initFrom(MediaFile source) {
        if (source.id != null) {
            this.id = source.id;
        }
        this.fullPath = source.fullPath;
        this.fileName = source.fileName;
        this.keywords = source.keywords;
        this.geoLocation = source.geoLocation;
        this.hash = source.hash;
        this.hashDate = source.hashDate;
        this.dimension = source.dimension;
        this.orientation = source.orientation;
        this.camera = source.camera;
        this.collectionId = source.collectionId;
        this.selected = source.selected;
        this.thumbnailType = source.thumbnailType;
        this.setLoadedInCache(false);
        this.setKeepOrThrow(source.getKeepOrThrow());
        this.setOriginalDate(source.getOriginalDate());
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
