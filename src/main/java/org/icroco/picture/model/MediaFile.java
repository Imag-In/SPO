package org.icroco.picture.model;

import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Callback;
import lombok.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

@Data
@EqualsAndHashCode(of = { "fullPath" })
@Builder
@AllArgsConstructor
public class MediaFile implements IMediaFile {
    public static  Comparator<MediaFile> UPDATED_COMP = Comparator.comparing(MediaFile::getHash);
    private static LocalDateTime         TIMESTAMP    = LocalDateTime.MIN.plusHours(1L);

    private Long           id;
    private Path           fullPath;
    private String         fileName;
    private LocalDateTime  originalDate;
    private Set<Keyword>   keywords;
    private GeoLocation    geoLocation;
    private String         hash;
    private LocalDate      hashDate;
    private Dimension      dimension;
    private Short          orientation;
    private Camera         camera;
    private Integer        collectionId;
    private boolean        selected;
    @Builder.Default
    private EKeepOrThrow   keepOrThrow   = EKeepOrThrow.UNKNOW;
    @NonNull
    @Builder.Default
    private EThumbnailType thumbnailType = EThumbnailType.ABSENT;


    //    @NonNull
//    @Builder.Default
//    private SimpleObjectProperty<Thumbnail> thumbnail = new SimpleObjectProperty<>(null);
    @NonNull
    @Builder.Default
    private final SimpleObjectProperty<LocalDateTime> lastUpdated   = new SimpleObjectProperty<>(TIMESTAMP);
    @NonNull
    @Builder.Default
    private final SimpleBooleanProperty               loadedInCache = new SimpleBooleanProperty(false);

    @NonNull
    @Builder.Default
    private SimpleLongProperty idProperty = new SimpleLongProperty(0);


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

    public static Callback<MediaFile, Observable[]> extractor() {
        return mf -> new Observable[] { mf.loadedInCache, mf.lastUpdated };
    }

    public void setId(Long id) {
        this.id = id;
        idProperty.set(id);
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
        this.originalDate = source.originalDate;
        this.keywords = source.keywords;
        this.geoLocation = source.geoLocation;
        this.hash = source.hash;
        this.hashDate = source.hashDate;
        this.dimension = source.dimension;
        this.orientation = source.orientation;
        this.camera = source.camera;
        this.collectionId = source.collectionId;
        this.selected = source.selected;
        this.keepOrThrow = source.keepOrThrow;
        this.thumbnailType = source.thumbnailType;
    }

    public void setNextKeepOrThrow() {
        keepOrThrow = keepOrThrow.next();
    }
}

//public record MediaFile(long id,
//                        Path fullPath,
//                        String fileName,
//                        LocalDate originalDate,
//                        Set<Tag> tags,
//                        SimpleObjectProperty<Thumbnail> thumbnail) implements IMediaFile {
//
//    @Builder
//    public MediaFile(long id, Path fullPath, String fileName, LocalDate originalDate, Set<Tag> tags, SimpleObjectProperty<Thumbnail> thumbnail) {
//        this.id = id;
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
//        return id == mediaFile.id;
//    }
//
//    public static Callback<MediaFile, Observable[]> extractor() {
//        return p -> new Observable[]{ p.thumbnail };
//    }
//}
