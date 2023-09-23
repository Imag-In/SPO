package org.icroco.picture.views.persistence;

import org.icroco.picture.persistence.model.DbTag;
import org.icroco.picture.views.AbstractDataTest;

public class DbTagTest extends AbstractDataTest<DbTag> {
    public static DbTag DUMMY = new DbTagTest().buildInstance();

    public DbTag buildInstance() {
        return DbTag.builder()
                .id(42)
                .name("DbPeople")
                .build();
    }

}