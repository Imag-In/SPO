package org.icroco.picture.model;

import lombok.Builder;

import java.io.Serializable;

/**
 * DTO for {@link org.icroco.picture.persistence.model.CameraEntity}
 */
@Builder
public record Camera(String make, String model) implements Serializable {
    public static Camera UNKWOWN_CAMERA = new Camera("_", "_");
}