package org.icroco.picture.infra.github;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

// https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface
public interface IGitHub {
    @GetExchange("/repos/{owner}/{repo}/releases/latest")
    GitHubRelease getLatestRelease(@PathVariable String owner, @PathVariable String repo);
}
