package org.icroco.picture.ui.model;

import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Callback;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

@Data
@EqualsAndHashCode(of = { "fullPath" })
@Builder
public class MediaFile implements IMediaFile {
    private long                            id;
    private Path                            fullPath;
    private String                          fileName;
    private LocalDate                       originalDate;
    private Set<Tag>                        tags;
    private SimpleObjectProperty<Thumbnail> thumbnail;

    public MediaFile(long id, Path fullPath, String fileName, LocalDate originalDate, Set<Tag> tags, SimpleObjectProperty<Thumbnail> thumbnail) {
        this.id = id;
        this.fullPath = fullPath;
        this.fileName = fileName;
        this.originalDate = originalDate;
        this.tags = tags;
        this.thumbnail = thumbnail == null ? new SimpleObjectProperty<>(null) : thumbnail;
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
    public LocalDate originalDate() {
        return getOriginalDate();
    }

    @Override
    public Set<Tag> tags() {
        return getTags();
    }

    public static Callback<MediaFile, Observable[]> extractor() {
        return mf -> new Observable[]{ mf.thumbnail };
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
////    public EThumbnailStatus status() {
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
