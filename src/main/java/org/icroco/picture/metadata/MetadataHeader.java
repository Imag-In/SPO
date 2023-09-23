package org.icroco.picture.metadata;

import lombok.Builder;
import org.icroco.picture.model.Dimension;
import org.icroco.picture.model.GeoLocation;

import java.time.LocalDateTime;

@Builder
public record MetadataHeader(LocalDateTime orginalDate,
                             Integer orientation,
                             Dimension size,
                             GeoLocation geoLocation) {
}
