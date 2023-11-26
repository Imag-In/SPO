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
@Table(name = "THUMBNAIL", uniqueConstraints = {
        @UniqueConstraint(name = "UK_THUMB_PATH", columnNames = { "FULL_PATH" }) })
@Slf4j
public class ThumbnailEntity {
    @Id
    Long mfId;

    @NotNull
    @Column(length = 1024, name = "FULL_PATH")
    @Type(PathType.class)
    private Path fullPath;

    @Lob
    @Column(name = "IMAGE", columnDefinition = "BLOB")
    private byte[] image;

    @NonNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EThumbnailType origin = EThumbnailType.ABSENT;

    @NonNull
    @Column(name = "LAST_UPDATE", columnDefinition = "DATE")
    private LocalDateTime lastUpdate;

}
