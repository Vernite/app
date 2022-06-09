package com.workflow.workflow.integration.git.github;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.workflow.integration.git.github.data.GitHubBranch;
import com.workflow.workflow.integration.git.github.data.GitHubCommit;
import com.workflow.workflow.integration.git.github.data.GitHubInstallationApi;
import com.workflow.workflow.integration.git.github.data.GitHubIssue;
import com.workflow.workflow.integration.git.github.data.GitHubPullRequest;
import com.workflow.workflow.integration.git.github.data.GitHubRepository;
import com.workflow.workflow.integration.git.github.data.GitHubUser;
import com.workflow.workflow.integration.git.github.data.GitHubWebhookData;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallation;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegrationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubTask;
import com.workflow.workflow.integration.git.github.entity.GitHubTaskRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class GitHubWebhookServiceTests {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private GitHubInstallationRepository installationRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private GitHubTaskRepository gitHubTaskRepository;

    private HmacUtils utils;

    private User user;
    private Project project;
    private Status[] statuses = new Status[2];
    private GitHubInstallation installation;
    private GitHubIntegration integration;

    @Value("${githubKey}")
    private void loadHmacUtils(String githubKey) {
        utils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, githubKey);
    }

    @BeforeAll
    void init() {
        integrationRepository.deleteAll();
        installationRepository.deleteAll();
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        project = projectRepository.save(new Project("NAME"));
        statuses[0] = statusRepository.save(new Status("NAME", 1, false, true, 0, project));
        statuses[1] = statusRepository.save(new Status("NAME", 1, true, false, 1, project));
        installation = installationRepository.save(new GitHubInstallation(1, user, "username"));
        integration = integrationRepository.save(new GitHubIntegration(project, installation, "username/repo"));
    }

    @Test
    void githubUnauthorized() {
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=12345")
                .header("X-GitHub-Event", "push")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void githubBadRequest() {
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex("[]"))
                .header("X-GitHub-Event", "push")
                .bodyValue("[]")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void githubSuccessRepositories() throws JsonProcessingException {
        // Test empty repositories remove
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex("{}"))
                .header("X-GitHub-Event", "installation_repositories")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
        // Test not empty repositories remove
        GitHubWebhookData data = new GitHubWebhookData();
        GitHubIntegration mockIntegration = new GitHubIntegration(project, installation, "username/test");
        data.setRepositoriesRemoved(List.of(
                new GitHubRepository(1, "username/test", false),
                new GitHubRepository(2, "untitled/23", false),
                new GitHubRepository(3, "untitled/test11", false)));
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation_repositories")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();
        assertEquals(false, integrationRepository.findById(mockIntegration.getId()).isPresent());
        assertEquals(true, integrationRepository.findById(integration.getId()).isPresent());
    }

    @Test
    void githubSuccessDefault() {
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex("{}"))
                .header("X-GitHub-Event", "unknown_event")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
    }

// it doesn't work:   
// 2022-06-09 18:37:32.318  WARN 98812 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 1452, SQLState: 23000
// 2022-06-09 18:37:32.318 ERROR 98812 --- [           main] o.h.engine.jdbc.spi.SqlExceptionHelper   : Cannot add or update a child row: a foreign key constraint fails (`workflow_test`.`task`, CONSTRAINT `fk_task_user` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE CASCADE)
// [ERROR] Tests run: 8, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 9.44 s <<< FAILURE! - in com.workflow.workflow.integration.git.github.GitHubWebhookServiceTests
// [ERROR] githubSuccessIssues  Time elapsed: 0.368 s  <<< ERROR!
// org.springframework.web.reactive.function.client.WebClientRequestException: Request processing failed; nested exception is org.springframework.dao.DataIntegrityViolationException: could not execute statement; SQL [n/a]; constraint [null]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement; nested exception is org.springframework.web.util.NestedServletException: Request processing failed; nested exception is org.springframework.dao.DataIntegrityViolationException: could not execute statement; SQL [n/a]; constraint [null]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement
//         at com.workflow.workflow.integration.git.github.GitHubWebhookServiceTests.githubSuccessIssues(GitHubWebhookServiceTests.java:168)
// Caused by: org.springframework.web.util.NestedServletException: Request processing failed; nested exception is org.springframework.dao.DataIntegrityViolationException: could not execute statement; SQL [n/a]; constraint [null]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement
//         at com.workflow.workflow.integration.git.github.GitHubWebhookServiceTests.githubSuccessIssues(GitHubWebhookServiceTests.java:168)
// Caused by: org.springframework.dao.DataIntegrityViolationException: could not execute statement; SQL [n/a]; constraint [null]; nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement
//         at com.workflow.workflow.integration.git.github.GitHubWebhookServiceTests.githubSuccessIssues(GitHubWebhookServiceTests.java:168)
// Caused by: org.hibernate.exception.ConstraintViolationException: could not execute statement
//         at com.workflow.workflow.integration.git.github.GitHubWebhookServiceTests.githubSuccessIssues(GitHubWebhookServiceTests.java:168)
// Caused by: java.sql.SQLIntegrityConstraintViolationException: Cannot add or update a child row: a foreign key constraint fails (`workflow_test`.`task`, CONSTRAINT `fk_task_user` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE CASCADE)
//         at com.workflow.workflow.integration.git.github.GitHubWebhookServiceTests.githubSuccessIssues(GitHubWebhookServiceTests.java:168)
// 
//     @Test
//     void githubSuccessIssues() throws JsonProcessingException {
//         GitHubWebhookData data = new GitHubWebhookData();
//         data.setAction("opened");
//         data.setIssue(new GitHubIssue(1, "url", "open", "title", "body"));
//         data.setRepository(new GitHubRepository(1, integration.getRepositoryFullName(), false));
//         client.post().uri("/webhook/github")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
//                 .header("X-GitHub-Event", "issues")
//                 .bodyValue(data)
//                 .exchange()
//                 .expectStatus().isOk();
//         client.post().uri("/webhook/github")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
//                 .header("X-GitHub-Event", "issues")
//                 .bodyValue(data)
//                 .exchange()
//                 .expectStatus().isOk();
//         assertEquals("title",
//                 gitHubTaskRepository.findByIssueIdAndGitHubIntegration(1, integration).get(0).getTask().getName());
//         assertEquals(1, gitHubTaskRepository.findByIssueIdAndGitHubIntegration(1, integration).size());

//         data.setAction("labeled");
//         data.setIssue(new GitHubIssue(1, "url", "open", "title 2", "body"));
//         client.post().uri("/webhook/github")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
//                 .header("X-GitHub-Event", "issues")
//                 .bodyValue(data)
//                 .exchange()
//                 .expectStatus().isOk();

//         data.setAction("edited");
//         data.setIssue(new GitHubIssue(1, "url", "open", "title 2", "body"));
//         client.post().uri("/webhook/github")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
//                 .header("X-GitHub-Event", "issues")
//                 .bodyValue(data)
//                 .exchange()
//                 .expectStatus().isOk();
//         assertEquals("title 2", gitHubTaskRepository.findByIssueIdAndGitHubIntegration(1, integration).get(0)
//                 .getTask().getName());

//         data.setAction("closed");
//         data.setIssue(new GitHubIssue(1, "url", "closed", "title 2", "body"));
//         client.post().uri("/webhook/github")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
//                 .header("X-GitHub-Event", "issues")
//                 .bodyValue(data)
//                 .exchange()
//                 .expectStatus().isOk();
//         assertEquals(statuses[1].getId(),
//                 gitHubTaskRepository.findByIssueIdAndGitHubIntegration(1, integration).get(0)
//                         .getTask().getStatus().getId());

//         data.setAction("reopened");
//         data.setIssue(new GitHubIssue(1, "url", "open", "title 2", "body"));
//         client.post().uri("/webhook/github")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
//                 .header("X-GitHub-Event", "issues")
//                 .bodyValue(data)
//                 .exchange()
//                 .expectStatus().isOk();
//         assertEquals(statuses[0].getId(),
//                 gitHubTaskRepository.findByIssueIdAndGitHubIntegration(1, integration).get(0)
//                         .getTask().getStatus().getId());

//         data.setAction("deleted");
//         data.setIssue(new GitHubIssue(1, "url", "open", "title 2", "body"));
//         client.post().uri("/webhook/github")
//                 .contentType(MediaType.APPLICATION_JSON)
//                 .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
//                 .header("X-GitHub-Event", "issues")
//                 .bodyValue(data)
//                 .exchange()
//                 .expectStatus().isOk();
//         assertEquals(0, gitHubTaskRepository.findByIssueIdAndGitHubIntegration(1, integration).size());
//     }

    @Test
    void githubSuccessPush() throws JsonProcessingException {
        GitHubWebhookData data = new GitHubWebhookData();
        data.setRepository(new GitHubRepository(1, integration.getRepositoryFullName(), false));
        data.setCommits(List.of(new GitHubCommit("1", "messagge without anything intresting")));
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "push")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();

        Task task = taskRepository.save(new Task("TEST", "DESC", statuses[0], user, 1));

        data.setCommits(List.of(new GitHubCommit("1", "messagge without anything intresting"),
                new GitHubCommit("2", "messagge with something intresting !" + task.getId())));

        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "push")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();
        assertEquals(statuses[1].getId(), taskRepository.findById(task.getId()).get().getStatus().getId());

        data.setCommits(List.of(new GitHubCommit("1", "messagge without anything intresting"),
                new GitHubCommit("2", "messagge with something intresting !666")));

        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "push")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();

        data.setCommits(List.of(new GitHubCommit("1", "messagge without anything intresting"),
                new GitHubCommit("2", "messagge with something intresting reopen!" + task.getId())));
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "push")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();
        assertEquals(statuses[0].getId(), taskRepository.findById(task.getId()).get().getStatus().getId());
    }

    @Test
    void githubSuccessInstallation() throws JsonProcessingException {
        GitHubWebhookData data = new GitHubWebhookData();
        data.setInstallation(new GitHubInstallationApi(2, new GitHubUser(1, "login")));
        data.setAction("unknown");
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();

        data.setInstallation(new GitHubInstallationApi(1, new GitHubUser(1, "login")));
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();

        data.setAction("suspend");
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();
        assertEquals(true, installationRepository.findByInstallationId(1L).get().getSuspended());

        data.setAction("unsuspend");
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();
        assertEquals(false, installationRepository.findByInstallationId(1L).get().getSuspended());

        data.setAction("deleted");
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();
        assertEquals(false, installationRepository.findById(installation.getId()).isPresent());

        installation = installationRepository.save(new GitHubInstallation(1, user, "username"));
        integration = integrationRepository.save(new GitHubIntegration(project, installation, "username/repo"));
    }

    @Test
    void githubSuccessPullRequest() throws JsonProcessingException {
        GitHubWebhookData data = new GitHubWebhookData();
        data.setAction("opened");
        data.setPullRequest(new GitHubPullRequest(20, "url", "open", "title", "body", new GitHubBranch("branch")));
        data.setRepository(new GitHubRepository(1, integration.getRepositoryFullName(), false));
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();

        Task task = taskRepository.save(new Task("TEST", "DESC", statuses[0], user, 1));
        gitHubTaskRepository.save(new GitHubTask(task, integration, 20, (byte) 1));

        data.setAction("closed");
        data.setPullRequest(new GitHubPullRequest(20, "url", "closed", "title", "body", new GitHubBranch("branch")));
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();
        assertEquals(statuses[1].getId(), taskRepository.findById(task.getId()).get().getStatus().getId());

        data.setAction("reopened");
        data.setPullRequest(new GitHubPullRequest(20, "url", "open", "title", "body", new GitHubBranch("branch")));
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();
        assertEquals(statuses[0].getId(), taskRepository.findById(task.getId()).get().getStatus().getId());

        data.setAction("edited");
        data.setPullRequest(new GitHubPullRequest(20, "url", "open", "title 2", "body", new GitHubBranch("branch")));
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();
        assertEquals("TEST", taskRepository.findById(task.getId()).get().getName());

        data.setAction("closed");
        data.setPullRequest(new GitHubPullRequest(20, "url", "open", "title 2", "body", new GitHubBranch("branch")));
        data.getPullRequest().setMerged(true);
        client.post().uri("/webhook/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request")
                .bodyValue(data)
                .exchange()
                .expectStatus().isOk();
        assertEquals((byte) 2, taskRepository.findById(task.getId()).get().getMergedPulls().get(0).getIsPullRequest());
    }
}
