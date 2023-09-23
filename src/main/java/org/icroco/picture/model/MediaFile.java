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
import java.util.Set;

@Data
@EqualsAndHashCode(of = { "fullPath" })
@Builder
@AllArgsConstructor
public class MediaFile implements IMediaFile {
    private long          id;
    private Path          fullPath;
    private String        fileName;
    private LocalDateTime originalDate;
    private Set<Tag>      tags;
    private GeoLocation   geoLocation;
    private String        hash;
    private LocalDate     hashDate;
    private Dimension     dimension;

    @NonNull
    @Builder.Default
    private EThumbnailType thumbnailType = EThumbnailType.ABSENT;

    //    @NonNull
//    @Builder.Default
//    private SimpleObjectProperty<Thumbnail> thumbnail = new SimpleObjectProperty<>(null);
    @NonNull
    @Builder.Default
    private final SimpleObjectProperty<LocalDateTime> thumbnailUpdateProperty = new SimpleObjectProperty<>(LocalDateTime.MIN.plusHours(1L));
    @NonNull
    @Builder.Default
    private final SimpleBooleanProperty               loadedInCache           = new SimpleBooleanProperty(false);

    @NonNull
    @Builder.Default
    private SimpleLongProperty idProperty = new SimpleLongProperty(0);

    private boolean selected;


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
    public Set<Tag> tags() {
        return getTags();
    }

    @Override
    public GeoLocation geoLocation() {
        return getGeoLocation();
    }

    @Override
    public Dimension dimension() {
        return getDimension();
    }

    public Set<Tag> getTags() {
        return tags == null ? Collections.emptySet() : tags;
    }

    public void invertSelection() {
        selected = !selected;
    }

    public static Callback<MediaFile, Observable[]> extractor() {
        return mf -> new Observable[]{ mf.loadedInCache, mf.thumbnailUpdateProperty };
    }

    public void setId(long id) {
        this.id = id;
        idProperty.set(id);
    }

    public void setLoadedInCache(boolean value) {
        loadedInCache.set(value);
    }

    public boolean isLoadedInCache() {
        return loadedInCache.get();
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
