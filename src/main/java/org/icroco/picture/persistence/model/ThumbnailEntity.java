package org.icroco.picture.persistence.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.icroco.picture.model.EThumbnailType;
import org.springframework.lang.NonNull;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "thumbnail")
@Slf4j
public class ThumbnailEntity {
    @Id
    Long mfId;

    @NotNull
    @Column(length = 1024, unique = true)
    @Type(PathType.class)
    private Path fullPath;

    @Lob
    @Column(name = "image", columnDefinition = "BLOB")
    private byte[] image;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EThumbnailType origin = EThumbnailType.ABSENT;

    @NonNull
    @Column(name = "last_update", columnDefinition = "DATE")
    private LocalDateTime lastUpdate;

}