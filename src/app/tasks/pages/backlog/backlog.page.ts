import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TaskService } from '@tasks/services/task/task.service';
import { TaskFilters } from '@tasks/filters/task.filters';
import { Observable, of } from 'rxjs';
import { Task } from '@tasks/interfaces/task.interface';
import { FormControl } from '@ngneat/reactive-forms';
import { ProjectService } from '@dashboard/services/project/project.service';
import { MemberService } from '@dashboard/services/member/member.service';
import { Project } from '@dashboard/interfaces/project.interface';
import { ProjectMember } from '@dashboard/interfaces/project-member.interface';
import { Status } from '@tasks/interfaces/status.interface';
import { StatusService } from '@tasks/services/status/status.service';
import { SprintService } from '@tasks/services/sprint.service';
import { Sprint } from '@tasks/interfaces/sprint.interface';
import { SprintFilters } from '@dashboard/filters/sprint.filters';
import { SprintStatus } from '@tasks/enums/sprint-status.enum';

@Component({
  selector: 'backlog-page',
  templateUrl: './backlog.page.html',
  styleUrls: ['./backlog.page.scss'],
})
export class BacklogPage implements OnInit {
  public projectId!: number;
  public project$: Observable<Project> = of();
  public filters = [];
  public filtersControl = new FormControl();

  public taskList$: Observable<Task[]> = of([]);
  public statusList$: Observable<Status[]> = of([]);
  public sprintListActive$: Observable<Sprint[]> = of([]);
  public sprintListCreated$: Observable<Sprint[]> = of([]);
  public emptyMap: Map<number, ProjectMember> = new Map();

  public members$?: Observable<Map<number, ProjectMember>> = of(this.emptyMap);

  constructor(
    private activatedRoute: ActivatedRoute,
    private taskService: TaskService,
    private projectService: ProjectService,
    private memberService: MemberService,
    private statusService: StatusService,
    private sprintService: SprintService,
  ) {}

  ngOnInit() {
    this.activatedRoute.params.subscribe(({ projectId }) => {
      this.projectId = projectId;

      this.taskList$ = this.taskService.backlog(projectId, [TaskFilters.BACKLOG()]);
      this.project$ = this.projectService.get(projectId);
      this.members$ = this.memberService.map(projectId);
      this.statusList$ = this.statusService.list(projectId);

      this.sprintListActive$ = this.sprintService.list(
        projectId,
        SprintFilters.STATUS(SprintStatus.ACTIVE),
      );
      this.sprintListCreated$ = this.sprintService.list(
        projectId,
        SprintFilters.STATUS(SprintStatus.CREATED),
      );
    });
  }
}
