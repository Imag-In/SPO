package org.icroco.picture.metadata;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

public interface IMetadataWriter {

    void setOrignialDate(Path path, LocalDateTime date);

    void setOrientation(Path path, int orientation);

    void setKeywords(Path path, Set<String> keywords);

    void addKeywords(Path path, Set<String> keywords);

    void removeKeywords(Path path, Set<String> keywords);

}
