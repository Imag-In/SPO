package org.icroco.picture.persistence.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Type;
import org.icroco.picture.model.EKeepOrThrow;
import org.icroco.picture.model.EThumbnailType;
import org.icroco.picture.persistence.converter.KeepOrThrowConverter;
import org.icroco.picture.persistence.converter.ThumbnailTypeConverter;

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
@Table(name = "MEDIA",
       indexes = @Index(name = "idx_mf_hash", columnList = "HASH"))
public class MediaFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    @Column(name = "FULL_PATH", length = 1024, unique = true)
    @Type(PathType.class)
    private Path fullPath;

    @NotNull
    @Column(name = "FILE_NAME", length = 128)
    private String fileName;

    @NotNull
    @Column(name = "ORIGINAL_DATE")
    private LocalDateTime originalDate;

    @Column(name = "HASH")
    private String hash;

    @Column(name = "HASH_CREATION", columnDefinition = "DATE")
    private LocalDate hashDate;

    @Column(name = "LAST_ACCESS", columnDefinition = "DATE")
    private LocalDate lastAccess;

    @Column(name = "COLLECTION_ID")
    private Integer collectionId;
    //    @ManyToOne(fetch = FetchType.LAZY)

    //    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER, mappedBy = "entities")
//    @JoinTable(name = "MF_TAGS", joinColumns = @JoinColumn(name = "tag_id"))
//    @JoinColumn(name = "tag_id", referencedColumnName = "id")
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "MF_KEYWORDS",
               joinColumns = @JoinColumn(name = "MF_ID"),
               inverseJoinColumns = @JoinColumn(name = "KW_ID"))
    private Set<KeywordEntity> keywords;

    @NonNull
    @Column(name = "THUMBNAIL_TYPE")
    @Convert(converter = ThumbnailTypeConverter.class)
    @Builder.Default
    private EThumbnailType thumbnailType = EThumbnailType.ABSENT;

    @NotNull
    private DimensionEntity dimension;

    @NotNull
    private GeoLocationEntity geoLocation;

    @Builder.Default
    @Column(name = "ORIENTATION", columnDefinition = "TINYINT")
    private Short orientation = 0;

    @NotNull
    private CameraEntity camera;

    @Builder.Default
    @Column(name = "KEEP_OR_THROW", columnDefinition = "TINYINT")
    @Convert(converter = KeepOrThrowConverter.class)
    private EKeepOrThrow keepOrThrow = EKeepOrThrow.UNKNOW;
}
