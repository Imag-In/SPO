package org.icroco.picture.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeoLocationEntity {
    static GeoLocationEntity EMPTY_GEO_LOC = new GeoLocationEntity(0D, 0D);

    @Column(name = "latitude", scale = 10)
    private double latitude;
    @Column(name = "longitude", scale = 10)
    private double longitude;
}
