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
public class CameraEntity {
    @Column(name = "MAKE")
    private String make;
    @Column(name = "MODEL")
    private String model;
}
