package org.icroco.picture.persistence.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.nio.file.Path;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "MEDIA_COLLECTION_ENTRY", uniqueConstraints = {
        @UniqueConstraint(name = "UK_THUMB_PATH", columnNames = { "PATH" }) })
public class MediaCollectionEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    Long id;

    @Column(name = "PATH", length = 1024, nullable = false)
    @Type(PathType.class)
    private Path name;

    @Column(name = "SUB_PATH_ID")
    Integer mcId;
}
