package org.icroco.picture.metadata;

import lombok.Builder;
import org.icroco.picture.model.*;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record MetadataHeader(LocalDateTime orginalDate,
                             Short orientation,
                             Dimension size,
                             GeoLocation geoLocation,
                             Camera camera,
                             ERating rating,

                             Set<Keyword> keywords) {
}
