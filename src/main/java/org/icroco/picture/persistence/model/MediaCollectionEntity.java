package org.icroco.picture.persistence.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;
import org.icroco.picture.persistence.converter.PathEntityConverter;

import java.nio.file.Path;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "MEDIA_COLLECTION")
public class MediaCollectionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    Integer id;

    @NotNull
    @Column(name = "PATH", length = 1024, unique = true)
    @Convert(converter = PathEntityConverter.class)
    @Type(PathType.class)
    private Path path;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "COLLECTION_ID")
    private Set<MediaFileEntity> medias;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Type(PathType.class)
    @JoinColumn(name = "SUB_PATH_ID")
    protected Set<MediaCollectionEntryEntity> subPaths;
}
