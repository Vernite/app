/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2023, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.vernite.vernite.cdn.File;
import dev.vernite.vernite.common.constants.DescriptionConstants;
import dev.vernite.vernite.common.constants.IDConstants;
import dev.vernite.vernite.common.constants.NameConstants;
import dev.vernite.vernite.common.constants.NullMessages;
import dev.vernite.vernite.common.utils.counter.CounterSequence;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegration;
import dev.vernite.vernite.meeting.Meeting;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.release.Release;
import dev.vernite.vernite.sprint.Sprint;
import dev.vernite.vernite.status.Status;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.SoftDeleteEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Entity for representing project.
 */
@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class Project extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero(message = IDConstants.NEGATIVE_MESSAGE)
    private long id;

    @NotBlank(message = NameConstants.BLANK_MESSAGE)
    @Column(nullable = false, length = NameConstants.MAX_LENGTH)
    @Size(min = NameConstants.MIN_LENGTH, max = NameConstants.MAX_LENGTH, message = NameConstants.SIZE_MESSAGE)
    private String name;

    @NotNull(message = DescriptionConstants.NULL_MESSAGE)
    @Column(nullable = false, length = DescriptionConstants.MAX_LENGTH)
    @Size(max = DescriptionConstants.MAX_LENGTH, message = DescriptionConstants.SIZE_MESSAGE)
    private String description;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "project")
    @NotNull(message = NullMessages.MEMBER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<ProjectWorkspace> projectWorkspaces = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull(message = NullMessages.USER)
    @JoinTable(name = "project_workspace", joinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "workspace_user_id", referencedColumnName = "id"))
    private Set<User> users = new HashSet<>();

    @ToString.Exclude
    @OrderBy("ordinal")
    @EqualsAndHashCode.Exclude
    @NotNull(message = NullMessages.STATUS)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "project")
    private List<Status> statuses = new ArrayList<>();

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull(message = NullMessages.COUNTER)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToOne(cascade = CascadeType.PERSIST, optional = false)
    private CounterSequence taskCounter;

    @NotNull
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "project")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<ProjectIntegration> githubProjectIntegrations = new ArrayList<>();

    @JsonIgnore
    @ToString.Exclude
    @OrderBy("startDate")
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "project")
    @NotNull(message = NullMessages.SPRINT)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Sprint> sprints = new ArrayList<>();

    @JsonIgnore
    @ToString.Exclude
    @OrderBy("deadline DESC")
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "project")
    @NotNull(message = NullMessages.RELEASE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Release> releases = new ArrayList<>();

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OrderBy("startDate, endDate")
    @OneToMany(mappedBy = "project")
    @NotNull(message = NullMessages.MEETING)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Meeting> meetings = new ArrayList<>();

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private File logo;

    /**
     * Default constructor.
     *
     * @param name        name of new project
     * @param description description of new project
     */
    public Project(String name, String description) {
        setName(name);
        setDescription(description);
        this.taskCounter = new CounterSequence();
    }

    /**
     * Constructor from create request.
     *
     * @param create body of create request
     */
    public Project(CreateProject create) {
        this(create.getName(), create.getDescription());
    }

    /**
     * Updates project with data from update.
     *
     * @param update body of update request
     */
    public void update(UpdateProject update) {
        Optional.ofNullable(update.getName()).ifPresent(this::setName);
        Optional.ofNullable(update.getDescription()).ifPresent(this::setDescription);
    }

    /**
     * Checks whether user is member of project.
     *
     * @param user potential project member
     * @return {@literal true} if given user is member of project, {@literal false}
     *         otherwise
     */
    public boolean isMember(User user) {
        return getUsers().contains(user);
    }

    /**
     * Remove user from project members.
     *
     * @param user must not be {@literal null}
     * @return removed connection; can be null if user wasn't member
     */
    public ProjectWorkspace removeMember(User user) {
        ProjectWorkspace removed = getProjectWorkspaces().stream()
                .filter(pw -> pw.getId().getWorkspaceId().getUserId() == user.getId()).findFirst().orElse(null);
        getProjectWorkspaces().remove(removed);
        return removed;
    }

    /**
     * Find index of user in project workspace list.
     *
     * @param user must not be {@literal null}. Must be value returned by
     *             repository.
     * @return index in project workspaces with given user or -1 when not found.
     */
    @Deprecated
    public int member(User user) {
        ListIterator<ProjectWorkspace> iterator = projectWorkspaces.listIterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId().getWorkspaceId().getUserId() == user.getId()) {
                return iterator.nextIndex() - 1;
            }
        }
        return -1;
    }

    /**
     * Setter for name value. It performs {@link String#trim()} on its argument.
     *
     * @param name new name value
     */
    public void setName(String name) {
        this.name = name.trim();
    }

    /**
     * Setter for description value. It performs {@link String#trim()} on its
     * argument.
     *
     * @param description new description value
     */
    public void setDescription(String description) {
        this.description = description.trim();
    }

    @Deprecated
    public Project(String name) {
        this(name, "");
        this.statuses.add(new Status("To Do", 0, 0, false, true, this));
        this.statuses.add(new Status("In Progress", 0, 1, false, false, this));
        this.statuses.add(new Status("Done", 0, 2, true, false, this));
    }

    @Deprecated
    public String getGitHubIntegration() {
        if (githubProjectIntegrations.isEmpty()) {
            return null;
        } else {
            return githubProjectIntegrations.get(0).getRepositoryFullName();
        }
    }

}
