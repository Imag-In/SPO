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
public class DimensionEntity {
    static DimensionEntity EMPTY_DIM = new DimensionEntity(0, 0);

    @Column(name = "WIDTH")
    private int width;

    @Column(name = "HEIGHT")
    private int height;
}
