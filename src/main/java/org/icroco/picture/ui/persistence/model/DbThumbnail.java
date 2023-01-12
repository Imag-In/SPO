package org.icroco.picture.ui.persistence.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "hash_access", columnDefinition = "DATE")
    private LocalDate hashDate;

    @Column(name = "last_access", columnDefinition = "DATE")
    private LocalDate lastAccess;

    @Lob
    @Column(name = "thumbnail", columnDefinition = "BLOB")
    byte[] thumbnail;
}
