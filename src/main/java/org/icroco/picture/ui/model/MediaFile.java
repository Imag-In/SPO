package org.icroco.picture.ui.model;

import javafx.beans.Observable;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Callback;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.icroco.picture.ui.util.NestedObjectProperty;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@EqualsAndHashCode(of = { "fullPath" })
@Builder
public class MediaFile implements IMediaFile {
    private long                            id;
    private Path                            fullPath;
    private String                          fileName;
    private LocalDateTime                   originalDate;
    private Set<Tag>                        tags;
    private SimpleObjectProperty<Thumbnail> thumbnail;
    private boolean                         selected;


    public MediaFile(long id,
                     Path fullPath,
                     String fileName,
                     LocalDateTime originalDate,
                     Set<Tag> tags,
                     SimpleObjectProperty<Thumbnail> thumbnail,
                     boolean isSelected) {
        this.id = id;
        this.fullPath = fullPath;
        this.fileName = fileName;
        this.originalDate = originalDate;
        this.tags = tags;
        this.thumbnail = thumbnail == null ? new SimpleObjectProperty<>(null) : thumbnail;
        this.selected = false;
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
    public Set<Tag> tags() {
        return getTags();
    }

    public void invertSelection() {
        selected = !selected;
    }

    public static Callback<MediaFile, Observable[]> extractor() {
        return mf -> new Observable[]{ mf.thumbnail, new NestedObjectProperty<>(mf.thumbnail, Thumbnail::getImageProperty) };
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
