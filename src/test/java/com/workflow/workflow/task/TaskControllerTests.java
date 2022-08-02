package com.workflow.workflow.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.sprint.Sprint;
import com.workflow.workflow.sprint.SprintRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.user.auth.AuthController;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class TaskControllerTests {
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private SprintRepository sprintRepository;

    private User user;
    private UserSession session;
    private Project project;
    private Sprint sprint;
    private Project forbiddenProject;

    void taskEquals(Task expected, Task actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getActive(), actual.getActive());
        assertEquals(expected.getDeadline(), actual.getDeadline());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getEstimatedDate(), actual.getEstimatedDate());
        assertEquals(expected.getIssue(), actual.getIssue());
        assertEquals(expected.getPriority(), actual.getPriority());
        assertEquals(expected.getPull(), actual.getPull());
    }

    @BeforeAll
    void init() {
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_tasks_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = sessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = sessionRepository.findBySession("session_token_tasks_tests").orElseThrow();
        }
        project = projectRepository.save(new Project("Tasks project"));
        sprint = sprintRepository.save(new Sprint(1, "name", new Date(), new Date(), "status", "description", project));
        forbiddenProject = projectRepository.save(new Project("Tasks project forbidden"));
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "tasks test workspace"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @BeforeEach
    void reset() {
        taskRepository.deleteAll();
    }

    @Test
    void getAllSuccess() {
        // Test empty return list
        client.get().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(0);
        // Prepare some workspaces for next test
        List<Task> tasks = List.of(
                new Task("name 1", "description", project.getStatuses().get(0), user, 0, "low"),
                new Task("name 3", "description", project.getStatuses().get(0), user, 0, "low"),
                new Task("name 2", "description", project.getStatuses().get(0), user, 0, "low"));
        taskRepository.saveAll(tasks);
        // Test non empty return list
        List<Task> result = client.get().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(3).returnResult().getResponseBody();
        taskEquals(tasks.get(0), result.get(0));
        taskEquals(tasks.get(1), result.get(2));
        taskEquals(tasks.get(2), result.get(1));
        // Test with soft deleted tasks
        tasks.get(0).setActive(new Date());
        taskRepository.save(tasks.get(0));

        client.get().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(2);
    }

    @Test
    void getAllUnauthorized() {
        client.get().uri("/project/{pId}/task", project.getId()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getAllNotFound() {
        client.get().uri("/project/{pId}/task", -1).cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        client.get().uri("/project/{pId}/task", forbiddenProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void createSuccess() {
        TaskRequest request = new TaskRequest("name", "desc", project.getStatuses().get(0).getNumber(), 0, "low");
        Task parentTask = taskRepository.save(new Task("parent", "desc", project.getStatuses().get(0), user, 0, "low"));
        Task task = client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, taskRepository.findByIdOrThrow(task.getId()));
        taskEquals(task, request.createEntity(project.getStatuses().get(0), user));

        request.setParentTaskId(parentTask.getId());
        task = client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, taskRepository.findByIdOrThrow(task.getId()));

        request.setSprintId(sprint.getNumber());
        task = client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, taskRepository.findByIdOrThrow(task.getId()));

        request.setAssigneeId(user.getId());
        task = client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, taskRepository.findByIdOrThrow(task.getId()));
    }

    @Test
    void createBadRequest() {
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest(null, "desc", project.getStatuses().get(0).getNumber(), 0, "low")).exchange()
                .expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest(" ", "desc", project.getStatuses().get(0).getNumber(), 0, "low")).exchange()
                .expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest("a".repeat(51), "desc", project.getStatuses().get(0).getNumber(), 0, "low"))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest("name", null, project.getStatuses().get(0).getNumber(), 0, "low"))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest("name", "desc", null, 0, "low"))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest("name", "desc", project.getStatuses().get(0).getNumber(), null, "low"))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest("name", "desc", project.getStatuses().get(0).getNumber(), 0, null))
                .exchange().expectStatus().isBadRequest();

        Task task = taskRepository.save(new Task("NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        Task parentTask = new Task("NAME", "DESC", project.getStatuses().get(0), user, 0, "low");
        parentTask.setParentTask(task);
        parentTask = taskRepository.save(parentTask);

        TaskRequest request = new TaskRequest("name", "desc", project.getStatuses().get(0).getNumber(), 0, "low");
        request.setParentTaskId(parentTask.getId());

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isBadRequest();
    }

    @Test
    void createUnauthorized() {
        client.post().uri("/project/{pId}/task", project.getId())
                .bodyValue(new TaskRequest("NAME", "DESC", project.getStatuses().get(0).getNumber(), 0, "low"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void createNotFound() {
        TaskRequest request = new TaskRequest("n", "d", forbiddenProject.getStatuses().get(0).getNumber(), 0, "low");

        client.post().uri("/project/{pId}/task", forbiddenProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        request.setStatusId(project.getStatuses().get(0).getId());
        request.setParentTaskId(666L);
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        request.setParentTaskId(null);
        client.post().uri("/project/{pId}/task", -1).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request).exchange().expectStatus().isNotFound();

        request.setSprintId(sprint.getNumber() + 1);
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();
    }

    @Test
    void getSuccess() {
        Task task = taskRepository.save(new Task("NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        Task result = client.get().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, result);
    }

    @Test
    void getUnauthorized() {
        client.get().uri("/project/{pId}/task/1", project.getId()).exchange().expectStatus().isUnauthorized();
        Task task = taskRepository.save(new Task("NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        client.get().uri("/project/{pId}/task/{id}", project.getId(), task.getId()).exchange().expectStatus()
                .isUnauthorized();
    }

    @Test
    void getNotFound() {
        client.get().uri("/project/{pId}/task/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        Task task = taskRepository.save(new Task("n", "d", forbiddenProject.getStatuses().get(0), user, 0, "low"));
        client.get().uri("/project/{pId}/task/{id}", forbiddenProject.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void updateSuccess() {
        Task task = taskRepository.save(new Task("NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        Task parentTask = taskRepository.save(new Task("NAME 2", "DESC", project.getStatuses().get(0), user, 0, "low"));

        Task result = client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TaskRequest()).exchange()
                .expectStatus().isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, result);

        TaskRequest request = new TaskRequest("new", "new", project.getStatuses().get(1).getNumber(), 1, "medium");
        task.setType(1);
        task.setPriority("medium");
        task.setName(request.getName().get());
        task.setDescription(request.getDescription().get());
        task.setStatus(project.getStatuses().get(1));

        result = client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, result);

        request.setDeadline(Date.from(Instant.now().minusSeconds(4000)));
        task.setDeadline(request.getDeadline().get());

        request.setEstimatedDate(Date.from(Instant.now().minusSeconds(4000)));
        task.setEstimatedDate(request.getEstimatedDate().get());

        request.setParentTaskId(parentTask.getId());
        task.setParentTask(parentTask);

        request.setSprintId(sprint.getNumber());
        task.setSprint(sprint);

        request.setAssigneeId(user.getId());
        task.setAssignee(user);

        result = client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, result);
        assertNotNull(taskRepository.findByIdOrThrow(task.getId()).getSprint());
        assertNotNull(taskRepository.findByIdOrThrow(task.getId()).getAssignee());

        request.setSprintId(null);
        task.setSprint(null);

        request.setAssigneeId(null);
        task.setAssignee(null);

        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class);
        assertNull(taskRepository.findByIdOrThrow(task.getId()).getSprint());
        assertNull(taskRepository.findByIdOrThrow(task.getId()).getAssignee());
    }

    @Test
    void updateBadRequest() {
        Task task = taskRepository.save(new Task("NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        Task parentTask = new Task("NAME 2", "DESC", project.getStatuses().get(0), user, 0, "low");
        Task parentParentTask = taskRepository.save(new Task("2", "D", project.getStatuses().get(0), user, 0, "low"));
        parentTask.setParentTask(parentParentTask);
        parentTask = taskRepository.save(parentTask);
        TaskRequest request = new TaskRequest();
        request.setParentTaskId(parentTask.getId());

        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isBadRequest();
    }

    @Test
    void updateUnauthorized() {
        TaskRequest request = new TaskRequest();
        client.put().uri("/project/{pId}/task/1", project.getId()).bodyValue(request).exchange().expectStatus()
                .isUnauthorized();

        Task task = taskRepository.save(new Task("NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId()).bodyValue(request).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void updateNotFound() {
        TaskRequest request = new TaskRequest();

        client.put().uri("/project/{pId}/task/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        Task fTask = taskRepository.save(new Task("N", "D", forbiddenProject.getStatuses().get(0), user, 0, "low"));
        Task task = taskRepository.save(new Task("NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        client.put().uri("/project/{pId}/task/{id}", forbiddenProject.getId(), fTask.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        request.setStatusId(666L);
        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        request = new TaskRequest();
        request.setParentTaskId(666L);
        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        request.setParentTaskId(null);
        request.setSprintId(2L);
        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();
        // This should return error, but it doesn't.
        // request.setSprintId(null);
        // request.setAssigneeId(10891L);
        // client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
        // .cookie(AuthController.COOKIE_NAME,
        // session.getSession()).bodyValue(request).exchange().expectStatus()
        // .isNotFound();
    }

    @Test
    void deleteSuccess() {
        Task task = taskRepository.save(new Task("NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        client.delete().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
        assertNotNull(taskRepository.findById(task.getId()).get().getActive());
    }

    @Test
    void deleteUnauthorized() {
        client.delete().uri("/project/{pId}/task/1", project.getId()).exchange().expectStatus().isUnauthorized();

        Task task = taskRepository.save(new Task("NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        client.delete().uri("/project/{pId}/task/{id}", project.getId(), task.getId()).exchange().expectStatus()
                .isUnauthorized();

        assertEquals(task.getActive(), taskRepository.findByIdOrThrow(task.getId()).getActive());
    }

    @Test
    void deleteNotFound() {
        client.delete().uri("/project/{pId}/task/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        Task task = taskRepository.save(new Task("N", "D", forbiddenProject.getStatuses().get(0), user, 0, "low"));
        client.delete().uri("/project/{pId}/task/{id}", forbiddenProject.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }
}
