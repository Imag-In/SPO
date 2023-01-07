package org.icroco.picture.ui.persistence;

import jakarta.persistence.*;
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

    @Column(length = 1024, unique = true, nullable = false)
    @Type(PathType.class)
    private Path fullPath;

    @Column(length = 128, nullable = false)
    private String fileName;

    @Column(nullable = false)
    private LocalDate originalDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "tag_id")
    protected Set<DbTag> tags;
}
