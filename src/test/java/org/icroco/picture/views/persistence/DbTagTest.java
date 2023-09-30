package org.icroco.picture.views.persistence;

import org.icroco.picture.persistence.model.TagEntity;
import org.icroco.picture.views.AbstractDataTest;

public class DbTagTest extends AbstractDataTest<TagEntity> {
    public static TagEntity DUMMY = new DbTagTest().buildInstance();

    public TagEntity buildInstance() {
        return TagEntity.builder()
                        .id(42)
                        .name("DbPeople")
                        .build();
    }

}