package org.icroco.picture.ui.util.metadata;

import com.drew.lang.GeoLocation;
import lombok.Builder;
import org.icroco.picture.ui.model.Dimension;

import java.time.LocalDateTime;

@Builder
public record MetadataHeader(LocalDateTime orginalDate,
                             Integer orientation,
                             Dimension size,
                             GeoLocation geoLocation) {
}
