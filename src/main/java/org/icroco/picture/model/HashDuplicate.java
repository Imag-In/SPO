package org.icroco.picture.model;

import java.util.Collection;

public record HashDuplicate(String hash, Collection<MediaFile> files) {
}
