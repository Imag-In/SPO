package org.icroco.picture.ui.model;

import lombok.Builder;

import java.nio.file.Path;

@Builder
public record MediaCollectionEntry(long id, Path name) {}
