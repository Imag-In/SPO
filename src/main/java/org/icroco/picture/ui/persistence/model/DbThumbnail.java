package org.icroco.picture.ui.persistence.model;

import jakarta.persistence.*;
import lombok.*;
import org.icroco.picture.ui.model.EThumbnailStatus;
import org.springframework.lang.NonNull;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "thumbnail")
public class DbThumbnail {

    @Id
    long id;

    @Column(name = "hash")
    private String hash;

    @Column(name = "hash_creation", columnDefinition = "DATE")
    private LocalDate hashDate;

    @Column(name = "last_access", columnDefinition = "DATE")
    private LocalDate lastAccess;

    @Lob
    @Column(name = "thumbnail", columnDefinition = "BLOB")
    private byte[] thumbnail;

    @NonNull
    @Enumerated(EnumType.ORDINAL)
    private EThumbnailStatus origin = EThumbnailStatus.ABSENT;
}
