package org.icroco.picture.ui.model;

import javafx.scene.image.Image;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record Thumbnail(long id, LocalDate lastAccess, Image image) {}
