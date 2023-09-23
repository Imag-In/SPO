package org.icroco.picture.persistence.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;

import java.nio.file.Path;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "media_collection")
public class DbMediaCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @NotNull
    @Column(length = 1024, unique = true)
    @Convert(converter = DbPathConverter.class)
    @Type(PathType.class)
    private Path path;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "collection_id")
    private Set<DbMediaFile> medias;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Type(PathType.class)
    @JoinColumn(name = "sub_path_id")
    protected Set<DbMediaCollectionEntry> subPaths;
}
