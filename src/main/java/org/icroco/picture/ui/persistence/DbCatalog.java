package org.icroco.picture.ui.persistence;

import jakarta.persistence.*;
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
@Table(name = "catalog")
public class DbCatalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(length = 1024, unique = true, nullable = false)
    @Convert(converter = DbPathConverter.class)
    @Type(PathType.class)
    private Path path;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "collection_id")
    private Set<DbMediaFile> medias;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Type(PathType.class)
    @JoinColumn(name = "sub_path_id")
    protected Set<DbCatalogEntry> subPaths;
}
