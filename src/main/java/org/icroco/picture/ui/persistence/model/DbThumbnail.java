package org.icroco.picture.ui.persistence.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.icroco.picture.ui.model.EThumbnailType;
import org.springframework.lang.NonNull;

import java.nio.file.Path;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "thumbnail")
@Slf4j
public class DbThumbnail {

    @Id
    Long id;

    @NotNull
    @Column(length = 1024, unique = true)
    @Type(PathType.class)
    private Path fullPath;

    @Lob
    @Column(name = "image", columnDefinition = "BLOB")
    private byte[] image;

    @NonNull
    @Enumerated(EnumType.STRING)
    private EThumbnailType origin = EThumbnailType.ABSENT;

    @NonNull
    @Column(name = "embeddedAvailable")
    private boolean embeddedAvailable;
}
