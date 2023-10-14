package org.icroco.picture.persistence.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "keyword")
public class KeywordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @Column(name = "name", length = 64, unique = true)
    private String name;

    //    @ManyToMany(fetch = FetchType.LAZY)
    @ManyToMany(mappedBy = "keywords", fetch = FetchType.LAZY)
    private Set<MediaFileEntity> entities;
}
