package com.workflow.workflow.integration.git;

import com.workflow.workflow.integration.git.github.GitHubInstallation;
import com.workflow.workflow.integration.git.github.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.GitHubIntegrationInfo;
import com.workflow.workflow.integration.git.github.service.GitHubService;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GitIntegrationController {
    private static final String INTEGRATION_LINK = "https://github.com/apps/workflow-2022/installations/new";
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GitHubService service;
    @Autowired
    private GitHubInstallationRepository installationRepository;

    @GetMapping("/integration/github")
    GitHubIntegrationInfo getRepositories() {
        // TODO: get current user for now 1
        User user = userRepository.findById(1L).orElseThrow();
        return new GitHubIntegrationInfo(INTEGRATION_LINK, service.getRepositories(user));
    }

    @PostMapping("/integration/github")
    GitHubIntegrationInfo postRepositories(long installationId) {
        // TODO: get current user for now 1
        User user = userRepository.findById(1L).orElseThrow();
        installationRepository.save(new GitHubInstallation(installationId, user));
        return new GitHubIntegrationInfo(INTEGRATION_LINK, service.getRepositories(user));
    }
}
