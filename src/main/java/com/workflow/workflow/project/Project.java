package com.workflow.workflow.project;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.counter.CounterSequence;
import com.workflow.workflow.integration.git.github.GitHubIntegration;
import com.workflow.workflow.projectworkspace.ProjectMember;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.utils.SoftDeleteEntity;

import org.hibernate.annotations.Where;

/**
 * Entity for representing project. Project name cant be longer than 50
 * characters.
 */
@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@JsonInclude(Include.NON_NULL)
public class Project extends SoftDeleteEntity implements Comparable<Project> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 50)
    private String name;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
    private List<ProjectWorkspace> projectWorkspaces = new ArrayList<>();

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
    @OrderBy("ordinal")
    @Where(clause = "active is null")
    private List<Status> statuses = new ArrayList<>();

    @JsonIgnore
    @OneToOne(cascade = CascadeType.REMOVE, optional = false)
    private CounterSequence statusCounter;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.REMOVE, optional = false)
    private CounterSequence taskCounter;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.REMOVE, optional = false)
    private CounterSequence sprintCounter;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
    private GitHubIntegration gitHubIntegration;

    public Project() {
    }

    public Project(String name, CounterSequence statusCounter, CounterSequence taskCounter,
            CounterSequence sprintCounter) {
        this.name = name;
        this.statusCounter = statusCounter;
        this.taskCounter = taskCounter;
        this.sprintCounter = sprintCounter;
    }

    public Project(ProjectRequest request, CounterSequence statusCounter, CounterSequence taskCounter,
            CounterSequence sprintCounter) {
        this(request.getName(), statusCounter, taskCounter, sprintCounter);
    }

    /**
     * Applies changes contained in request object to project.
     * 
     * @param request must not be {@literal null}. Can contain {@literal null} in
     *                fields. If field is {@literal null} it is assumed there is no
     *                changes for that field.
     */
    public void apply(ProjectRequest request) {
        if (request.getName() != null) {
            name = request.getName();
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ProjectWorkspace> getProjectWorkspaces() {
        return projectWorkspaces;
    }

    public void setProjectWorkspaces(List<ProjectWorkspace> projectWorkspaces) {
        this.projectWorkspaces = projectWorkspaces;
    }

    public List<Status> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<Status> statuses) {
        this.statuses = statuses;
    }

    public CounterSequence getStatusCounter() {
        return statusCounter;
    }

    public void setStatusCounter(CounterSequence statusCounter) {
        this.statusCounter = statusCounter;
    }

    public CounterSequence getTaskCounter() {
        return taskCounter;
    }

    public void setTaskCounter(CounterSequence taskCounter) {
        this.taskCounter = taskCounter;
    }

    public CounterSequence getSprintCounter() {
        return sprintCounter;
    }

    public void setSprintCounter(CounterSequence sprintCounter) {
        this.sprintCounter = sprintCounter;
    }

    public String getGitHubIntegration() {
        return gitHubIntegration != null && gitHubIntegration.getActive() == null
                ? gitHubIntegration.getRepositoryFullName()
                : null;
    }

    public void setGitHubIntegration(GitHubIntegration gitHubIntegration) {
        this.gitHubIntegration = gitHubIntegration;
    }

    @JsonIgnore
    public List<ProjectMember> getProjectMembers() {
        return getProjectWorkspaces().stream().map(ProjectWorkspace::getProjectMember).toList();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + Long.hashCode(getId());
        hash = prime * hash + (getName() == null ? 0 : getName().hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Project other = (Project) obj;
        if (getId() != other.getId())
            return false;
        if (getName() == null)
            return other.name == null;
        return getName().equals(other.getName());
    }

    @Override
    public int compareTo(Project other) {
        return getName().equals(other.getName()) ? Long.compare(getId(), other.getId())
                : getName().compareTo(other.getName());
    }
}
