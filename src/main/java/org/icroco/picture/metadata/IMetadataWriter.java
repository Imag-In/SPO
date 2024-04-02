package org.icroco.picture.metadata;

import io.jbock.util.Either;
import org.icroco.picture.model.ERating;
import org.icroco.picture.model.ERotation;
import org.icroco.picture.model.Keyword;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

public interface IMetadataWriter {

    Either<Throwable, Path> setOrignialDate(Path path, LocalDateTime date);

    Either<Throwable, Path> setOrientation(Path path, ERotation orientation);

    void setKeywords(Path path, Set<Keyword> keywords);

    void addKeywords(Path path, Set<Keyword> keywords);

    void removeKeywords(Path path, Set<String> keywords);

    Either<Throwable, Path> setRating(Path path, ERating rating);

    Either<Throwable, Path> setThumbnail(Path path, byte[] thumbnail);

}
