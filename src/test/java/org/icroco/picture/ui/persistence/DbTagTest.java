package org.icroco.picture.ui.persistence;

import org.icroco.picture.ui.AbstractDataTest;

import static org.junit.jupiter.api.Assertions.*;

public class DbTagTest extends AbstractDataTest<DbTag> {
    public static DbTag DUMMY = new DbTagTest().buildInstance();

    public DbTag buildInstance() {
        return DbTag.builder()
                .id(42)
                .name("DbPeople")
                .build();
    }

}