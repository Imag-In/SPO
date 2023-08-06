package org.icroco.picture.ui.model;

import javafx.beans.Observable;
import javafx.beans.property.SimpleBooleanProperty;
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
    private       long                                 id;
    private       Path                                 fullPath;
    private       String                               fileName;
    private       LocalDateTime                        originalDate;
    private       Set<Tag>                             tags;
    private       String                               gps;
    private       String                               hash;
    private       LocalDate                            hashDate;
    //    @NonNull
//    @Builder.Default
//    private SimpleObjectProperty<Thumbnail> thumbnail = new SimpleObjectProperty<>(null);
    @NonNull
    @Builder.Default
    private final SimpleObjectProperty<EThumbnailType> thumbnailType = new SimpleObjectProperty<>(EThumbnailType.ABSENT);

    @NonNull
    @Builder.Default
    private SimpleBooleanProperty loaded = new SimpleBooleanProperty(false);

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

    public Set<Tag> getTags() {
        return tags == null ? Collections.emptySet() : tags;
    }

    public void invertSelection() {
        selected = !selected;
    }

    public static Callback<MediaFile, Observable[]> extractor() {
        return mf -> new Observable[]{ mf.loaded };
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    public void setLoaded(boolean value) {
        loaded.setValue(value);
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
