package com.workflow.workflow.integration.git.github.service;

import java.util.List;

public class GitHubWebhookData {
    private String action;
    private GitHubRepository repository;
    private GitHubInstallationApi installation;
    private List<GitHubRepository> repositoriesRemoved;
    private GitHubIssue issue;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public GitHubIssue getIssue() {
        return issue;
    }

    public void setIssue(GitHubIssue issue) {
        this.issue = issue;
    }

    public List<GitHubRepository> getRepositoriesRemoved() {
        return repositoriesRemoved;
    }

    public void setRepositoriesRemoved(List<GitHubRepository> repositoriesRemoved) {
        this.repositoriesRemoved = repositoriesRemoved;
    }

    public GitHubRepository getRepository() {
        return repository;
    }

    public void setRepository(GitHubRepository repository) {
        this.repository = repository;
    }

    public GitHubInstallationApi getInstallation() {
        return installation;
    }

    public void setInstallation(GitHubInstallationApi installation) {
        this.installation = installation;
    }
}
