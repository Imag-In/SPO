package org.icroco.picture.infra.github;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubRelease(int id, String name, @JsonProperty(value = "tag_name") String tagName) {
}
