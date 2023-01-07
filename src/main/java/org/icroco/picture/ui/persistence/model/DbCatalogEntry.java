package org.icroco.picture.ui.persistence.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.nio.file.Path;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "catalog_entry")
public class DbCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(length = 64, nullable = false)
    @Type(PathType.class)
    private Path name;
}
