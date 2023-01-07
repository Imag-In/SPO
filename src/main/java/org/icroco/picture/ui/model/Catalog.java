package org.icroco.picture.ui.model;

import lombok.Builder;

import java.nio.file.Path;
import java.util.Set;

@Builder
public record Catalog(int id, Path path, Set<MediaFile> medias, Set<CatalogueEntry> subPaths) {

}
