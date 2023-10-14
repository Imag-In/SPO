package org.icroco.picture.views.persistence;

import org.icroco.picture.persistence.model.KeywordEntity;
import org.icroco.picture.views.AbstractDataTest;

public class DbTagTest extends AbstractDataTest<KeywordEntity> {
    public static KeywordEntity DUMMY = new DbTagTest().buildInstance();

    public KeywordEntity buildInstance() {
        return KeywordEntity.builder()
                            .id(42)
                            .name("DbPeople")
                            .build();
    }

}