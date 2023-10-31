package org.icroco.picture.persistence.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;
import org.icroco.picture.model.EThumbnailType;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "media")
public class MediaFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    @Column(length = 1024, unique = true)
    @Type(PathType.class)
//    @Convert(converter = DbPathConverter.class)
    private Path fullPath;

    @NotNull
    @Column(length = 128)
    private String fileName;

    @NotNull
    @Column()
    private LocalDateTime originalDate;

    @Column(name = "hash")
    private String hash;

    @Column(name = "hash_creation", columnDefinition = "DATE")
    private LocalDate hashDate;

    @Column(name = "last_access", columnDefinition = "DATE")
    private LocalDate lastAccess;

    @Column(name = "collection_id")
    private Integer collectionId;
    //    @ManyToOne(fetch = FetchType.LAZY)

    //    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER, mappedBy = "entities")
//    @JoinTable(name = "MF_TAGS", joinColumns = @JoinColumn(name = "tag_id"))
//    @JoinColumn(name = "tag_id", referencedColumnName = "id")
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "mf_keywords",
               joinColumns = @JoinColumn(name = "mf_id"),
               inverseJoinColumns = @JoinColumn(name = "kw_id"))
    private Set<KeywordEntity> keywords;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EThumbnailType thumbnailType = EThumbnailType.ABSENT;

    @NotNull
    private DimensionEntity dimension;

    @NotNull
    private GeoLocationEntity geoLocation;

    @Builder.Default
    @Column(name = "orientation")
    private Short orientation = 0;

    @NotNull
    private CameraEntity camera;
}
