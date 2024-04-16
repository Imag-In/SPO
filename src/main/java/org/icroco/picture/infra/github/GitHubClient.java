package org.icroco.picture.infra.github;

import lombok.extern.slf4j.Slf4j;
import org.icroco.picture.event.NewVersionEvent;
import org.icroco.picture.views.task.TaskService;
import org.jooq.lambda.Unchecked;
import org.semver4j.Semver;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.URI;
import java.util.Optional;

import static java.time.Duration.ofSeconds;

@Component
@Slf4j
public class GitHubClient {
    public static final String          GITHUB_API_URL = "https://api.github.com";
    private final       TaskService     taskService;
    private final       BuildProperties buildProperties;
    private final       IGitHub         github;
    private final       Semver          currentVersion;
    private final       URI             releaseHome;

    public GitHubClient(TaskService taskService, BuildProperties buildProperties) {
        this.taskService = taskService;
        this.buildProperties = buildProperties;
        this.currentVersion = Semver.parse(buildProperties.getVersion());
        this.releaseHome = Unchecked.supplier(() -> new URI("https://github.com/Imag-In/SPO/releases")).get();

        RestClient restClient = RestClient.builder().baseUrl(GITHUB_API_URL).build();
        RestClientAdapter       adapter    = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory    = HttpServiceProxyFactory.builderFor(adapter).build();

        github = factory.createClient(IGitHub.class);
    }

    @EventListener(ApplicationStartedEvent.class)
    private void checkLatetestRelease() {
        Thread.ofVirtual().name("Release checking").start(Unchecked.runnable(() -> {
            Thread.sleep(ofSeconds(15)); // It cost nothing with Virtual Thread. // Put in conf.
            try {
                var latestVersion = github.getLatestRelease("Imag-In", "SPO");
                log.info("Current: '{}', Latest release: '{}'",
                         buildProperties.getVersion(),
                         latestVersion);
                Optional.ofNullable(Semver.parse(latestVersion.tagName()))
                        .or(() -> Optional.ofNullable(Semver.parse(latestVersion.name())))
                        .filter(v -> v.isGreaterThan(currentVersion))
                        .ifPresent(v -> taskService.sendEvent(NewVersionEvent.builder()
                                                                             .version(v.getVersion())
                                                                             .url(releaseHome)
                                                                             .source(this)
                                                                             .build()));
            } catch (Exception e) {
                log.warn("Cannot get latest version from: '{}'", GITHUB_API_URL);
            }
        }));
    }
}
