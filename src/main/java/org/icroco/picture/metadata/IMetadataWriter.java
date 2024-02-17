package org.icroco.picture.metadata;

import org.icroco.picture.model.Keyword;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

public interface IMetadataWriter {

    void setOrignialDate(Path path, LocalDateTime date);

    void setOrientation(Path path, int orientation);

    void setKeywords(Path path, Set<Keyword> keywords);

    void addKeywords(Path path, Set<Keyword> keywords);

    void removeKeywords(Path path, Set<String> keywords);

}
