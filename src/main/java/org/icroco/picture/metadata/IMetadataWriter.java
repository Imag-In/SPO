package org.icroco.picture.metadata;

import io.jbock.util.Either;
import org.icroco.picture.model.ERotation;
import org.icroco.picture.model.Keyword;
import org.icroco.picture.model.Thumbnail;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

public interface IMetadataWriter {

    Either<Exception, Path> setOrignialDate(Path path, LocalDateTime date);

    Either<Exception, Path> setOrientation(Path path, ERotation orientation);

    void setKeywords(Path path, Set<Keyword> keywords);

    void addKeywords(Path path, Set<Keyword> keywords);

    void removeKeywords(Path path, Set<String> keywords);

    Either<Exception, Path> setThumbnail(Path path, Thumbnail thumbnail);

}
