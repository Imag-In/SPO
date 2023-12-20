package org.icroco.picture.persistence.model;

public interface MfDuplicate {
    Long getId();

//    @Type(PathType.class)
//    Path getFull_Path();

    String getHash();
}
