package org.icroco.picture.infra.github;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;

@ConfigurationProperties(prefix = "imagin.spo.github")
public record GitHubProperties(@NonNull String appId, @NonNull Integer appInstallationId) {
}
