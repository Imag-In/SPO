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
public class DbDimension {
    static DbDimension EMPTY_DIM = new DbDimension(0, 0);

    @Column(name = "width")
    private int width;

    @Column(name = "height")
    private int height;
}
