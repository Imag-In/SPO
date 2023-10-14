package org.icroco.picture.model;

import lombok.Builder;

@Builder
public record Keyword(int id, String name) {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Keyword keyword = (Keyword) o;

        return id == keyword.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
