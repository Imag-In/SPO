package org.icroco.picture.infra.github;

import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

@Slf4j
public class GhIssueDemo {
    public static void main(String[] args) throws Exception {
        String
                jwtToken =
                GitHubClient.createJWT(GhIssueDemo.class.getResource("/spo-issue-creator.2024-04-17.private-key.der"),
                                       "879074",
                                       600000); //sdk-github-api-app-test
        log.info("JWT token: {}", jwtToken);
        GitHub gitHubApp = new GitHubBuilder().withJwtToken(jwtToken).build();
        log.info("gitHubApp: {}", gitHubApp.getApiUrl());

        GHAppInstallation appInstallation = gitHubApp.getApp().getInstallationById(49716371); // Installation Id
        var               token           = appInstallation.createToken().create();
        log.info("token: {}", token.getToken());
        gitHubApp = new GitHubBuilder().withJwtToken(token.getToken()).build();
        var orga = gitHubApp.getOrganization("Imag-In");
        var repo = gitHubApp.getRepository("Imag-In/SPO");
        log.info("Repo: {}", repo);
        var issue = repo.createIssue("TestFoo")
                        .body("bar")
                        .label("bug")
                        .create();
    }
}
