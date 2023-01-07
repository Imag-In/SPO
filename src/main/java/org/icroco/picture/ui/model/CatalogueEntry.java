package org.icroco.picture.ui.model;

import lombok.Builder;

import java.nio.file.Path;

@Builder
public record CatalogueEntry(long id, Path name) {}
