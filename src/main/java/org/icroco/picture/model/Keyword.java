package org.icroco.picture.model;

import lombok.Builder;

import java.util.Objects;

@Builder
public record Keyword(Integer id, String name) {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Keyword keyword = (Keyword) o;

        return Objects.equals(id, keyword.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
