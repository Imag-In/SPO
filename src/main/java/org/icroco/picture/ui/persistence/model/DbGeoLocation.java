package org.icroco.picture.ui.persistence.model;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbGeoLocation {
    static DbGeoLocation EMPTY_GEO_LOC = new DbGeoLocation(0, 0);

    private double latitude;
    private double longitude;
}
