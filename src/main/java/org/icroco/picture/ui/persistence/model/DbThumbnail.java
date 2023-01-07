package org.icroco.picture.ui.persistence.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

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

    @Column(name = "last_access", columnDefinition = "DATE")
    private LocalDate lastAccess;

    @Lob
    @Column(name = "thumbnail", columnDefinition="BLOB")
    byte[] thumbnail;
}
