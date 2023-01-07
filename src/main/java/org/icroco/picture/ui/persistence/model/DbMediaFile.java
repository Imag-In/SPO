package org.icroco.picture.ui.persistence.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "media")
public class DbMediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @NotNull
    @Column(length = 1024, unique = true)
    @Type(PathType.class)
    private Path fullPath;

    @NotNull
    @Column(length = 128)
    private String fileName;

    @NotNull
    @Column()
    private LocalDate originalDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "tag_id")
    protected Set<DbTag> tags;
}
