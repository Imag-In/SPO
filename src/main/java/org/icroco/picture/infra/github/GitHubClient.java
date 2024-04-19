package org.icroco.picture.infra.github;

import com.twelvemonkeys.lang.Platform;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.icroco.picture.event.NewVersionEvent;
import org.icroco.picture.event.NotificationEvent;
import org.icroco.picture.util.HumanReadableSize;
import org.icroco.picture.views.task.TaskService;
import org.jooq.lambda.Unchecked;
import org.kohsuke.github.GitHubBuilder;
import org.semver4j.Semver;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Optional;

import static java.time.Duration.ofSeconds;

@Component
@Slf4j
public class GitHubClient {
    public static final String           GITHUB_API_URL = "https://api.github.com";
    private final       TaskService      taskService;
    private final       BuildProperties  buildProperties;
    private final       GitHubProperties ghProperties;
    private final       IGitHub          github;
    private final       Semver           currentVersion;
    private final       URI              releaseHome;

    public GitHubClient(TaskService taskService, BuildProperties buildProperties, GitHubProperties ghProperties) {
        this.taskService = taskService;
        this.buildProperties = buildProperties;
        this.ghProperties = ghProperties;
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

    public void reportIssue(Throwable throwable) {
        Thread.ofVirtual().name("gh-report-issue").start(() -> {
            try {
                var jwtToken = createJWT(getClass().getResource("/spo-issue-creator.2024-04-17.private-key.der"),
                                         ghProperties.appId(),
                                         600000); //sdk-github-api-app-test
                var gitHubApp       = new GitHubBuilder().withJwtToken(jwtToken).build();
                var appInstallation = gitHubApp.getApp().getInstallationById(ghProperties.appInstallationId()); // Installation Id
                var token           = appInstallation.createToken().create();
                gitHubApp = new GitHubBuilder().withJwtToken(token.getToken()).build();
                var repo = gitHubApp.getRepository("Imag-In/SPO");
                try (var sw = new StringWriter(1024);
                     var pw = new PrintWriter(sw)) {
                    throwable.printStackTrace(pw);
                    var issue = repo.createIssue("Unexpected Error")
                                    .body(STR."""
                                      OS  : \{Platform.os()} / \{Platform.arch()} / \{Platform.version()}
                                      Core: \{Runtime.getRuntime().availableProcessors()}
                                      RAM : \{HumanReadableSize.decimalBased(Runtime.getRuntime()
                                                                                    .totalMemory())} / \{HumanReadableSize.decimalBased(Runtime.getRuntime()
                                                                                                                                               .maxMemory())}
                                      Java: \{SystemUtils.JAVA_VERSION} (\{SystemUtils.JAVA_CLASS_VERSION}) / \{SystemUtils.JAVA_VENDOR}

                                      Exception: \{throwable.getMessage()}
                                      \{sw.toString()}
                                      """)
                                    .label("bug")
                                    .create();
                    log.warn("Issue created: '{}'", issue.getHtmlUrl());
                    taskService.sendEvent(NotificationEvent.builder()
                                                           .type(NotificationEvent.NotificationType.INFO)
                                                           .message(STR."Issue reported: \{issue.getHtmlUrl()}")
                                                           .source(this)
                                                           .build());
                }
            } catch (Throwable t) {
                log.error("Cannot report issue to github: {}", throwable.getMessage(), t);
            }
        });
    }

    static PrivateKey get(String filename) throws Exception {
        return get(Files.readAllBytes(Path.of(filename)));
    }

    static PrivateKey get(byte[] keyBytes) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory          kf   = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    static String createJWT(URL resource, String githubAppId, long ttlMillis) throws Exception {
        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = Jwts.SIG.RS256;
        long               nowMillis          = System.currentTimeMillis();
        Date               now                = new Date(nowMillis);

        //We will sign our JWT with our private key
        Key signingKey = get(IOUtils.toByteArray(resource));

        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder()
                                 .issuedAt(now)
                                 .issuer(githubAppId)
                                 .signWith(signingKey);

        //if it has been specified, let's add the expiration
        if (ttlMillis > 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp       = new Date(expMillis);
            builder.expiration(exp);
        }

        //Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();
    }
}
