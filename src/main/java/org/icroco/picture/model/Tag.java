package org.icroco.picture.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
public record Tag(int id, String name) {}
