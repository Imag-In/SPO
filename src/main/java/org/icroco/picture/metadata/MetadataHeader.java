package org.icroco.picture.metadata;

import lombok.Builder;
import org.icroco.picture.model.Camera;
import org.icroco.picture.model.Dimension;
import org.icroco.picture.model.GeoLocation;
import org.icroco.picture.model.Keyword;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record MetadataHeader(LocalDateTime orginalDate,
                             Short orientation,
                             Dimension size,
                             GeoLocation geoLocation,
                             Camera camera,

                             Set<Keyword> keywords) {
}
