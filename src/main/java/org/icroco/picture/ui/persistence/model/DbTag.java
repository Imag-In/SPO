package org.icroco.picture.ui.persistence.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.nio.file.Path;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tag")
public class DbTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(length = 64, unique = true)
    private String name;
}
