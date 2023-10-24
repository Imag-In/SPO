package org.icroco.picture.event;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.nio.file.Path;

@Getter
@ToString
@SuperBuilder
public class ImportDirectoryEvent extends IiEvent {
    private final Path rootDirectory;
}
