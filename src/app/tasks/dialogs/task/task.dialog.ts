import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { GitIssue, GitPull } from '@dashboard/interfaces/git-integration.interface';
import { GitIntegrationService } from '@dashboard/services/git-integration/git-integration.service';
import { validateForm } from '@main/classes/form.class';
import { timeToInteraction } from '@main/classes/time-to-interaction.class';
import { RouterExtensionsService } from '@main/services/router-extensions/router-extensions.service';
import { FormControl, FormGroup } from '@ngneat/reactive-forms';
import { UntilDestroy, untilDestroyed } from '@ngneat/until-destroy';
import { TaskPriority } from '@tasks/enums/task-priority.enum';
import { TaskType } from '@tasks/enums/task-type.enum';
import { Status } from '@tasks/interfaces/status.interface';
import { Observable, EMPTY, tap, skip } from 'rxjs';
import { requiredValidator } from '../../../_main/validators/required.validator';
import { Task } from '../../interfaces/task.interface';
import { unixTimestamp } from '../../../_main/interfaces/date.interface';
import { StatusService } from '@tasks/services/status/status.service';
import { TaskService } from '@tasks/services/task/task.service';
import { ProjectService } from '@dashboard/services/project/project.service';
import { Enum } from '@main/classes/enum.class';
import { Loader } from '../../../_main/classes/loader/loader.class';
import { withLoader } from '@main/operators/loader.operator';

export enum TaskDialogVariant {
  CREATE = 'create',
  EDIT = 'edit',
}

export interface TaskDialogData {
  workspaceId: number;
  projectId?: number;
  variant: TaskDialogVariant;
  task?: Partial<Task>;
  parentTask?: Task;
}

@UntilDestroy()
@Component({
  selector: 'app-task-dialog',
  templateUrl: './task.dialog.html',
  styleUrls: ['./task.dialog.scss'],
})
export class TaskDialog implements OnInit {
  /** Task types list observable initialized with possible values from parentTask from data */
  public taskTypes$ = this.taskService.listTaskTypes(this.data.parentTask?.type);

  /** Priorities list */
  public taskPriorities = Enum.values(TaskPriority);

  public parentTask$ =
    this.data.projectId && this.data.task?.parentTaskId
      ? this.taskService.get(this.data.projectId, this.data.task?.parentTaskId)
      : EMPTY;
  public projectList$ = this.projectService.list();
  public statusList$!: Observable<Status[]>;
  public issueList$!: Observable<GitIssue[]>;
  public pullList$!: Observable<GitPull[]>;

  public isGitHubIntegrationAvailable: boolean = false;

  public form = new FormGroup({
    id: new FormControl<number | null>(null),
    parentTaskId: new FormControl<number | null>(null),
    type: new FormControl<TaskType>(this.taskTypes$.value[0][1], [requiredValidator()]),
    name: new FormControl<string>('', [requiredValidator()]),
    statusId: new FormControl<number | null>(null, [requiredValidator()]),
    projectId: new FormControl<number | null>(null, [requiredValidator()]),
    description: new FormControl<string>(''),
    priority: new FormControl<TaskPriority>(TaskPriority.MEDIUM, [requiredValidator()]),
    deadline: new FormControl<unixTimestamp | null>(null),
    estimatedDate: new FormControl<unixTimestamp | null>(null),
    issue: new FormControl<GitIssue | 'CREATE' | 'DETACH' | null>(null),
    pull: new FormControl<GitPull | 'DETACH' | null>(null),
    storyPoints: new FormControl<number | null>(0),
  });

  public interactive$ = timeToInteraction();

  public statusListLoader = new Loader();

  /** @ignore */
  TaskType = TaskType;

  /** @ignore */
  TaskDialogVariant = TaskDialogVariant;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: TaskDialogData,
    private dialogRef: MatDialogRef<TaskDialog>,
    private statusService: StatusService,
    private gitIntegrationService: GitIntegrationService,
    private routerExtensions: RouterExtensionsService,
    private taskService: TaskService,
    private projectService: ProjectService,
  ) {}

  ngOnInit() {
    const { projectId } = this.routerExtensions.snapshot.params;

    if (projectId) this.data.projectId = Number(projectId);

    this.form.patchValue({
      ...this.data.task,
      projectId: this.data.projectId,
    });

    if (this.data.projectId) this.onProjectIdChange(this.data.projectId);

    this.form
      .get('projectId')
      ?.valueChanges.pipe(skip(1), untilDestroyed(this))
      .subscribe((projectId) => {
        console.log('value changed');
        this.onProjectIdChange(projectId);
      });

    this.form
      .get('parentTaskId')
      ?.valueChanges.pipe(untilDestroyed(this))
      .subscribe((parentTaskId) => {
        if (parentTaskId && this.form.get('projectId').value) {
          this.parentTask$ = this.taskService.get(this.form.get('projectId').value!, parentTaskId);
        } else {
          this.parentTask$ = EMPTY;
        }
      });

    console.log(this.form.get('parentTaskId').value, this.form.get('projectId').value);
    if (this.form.get('parentTaskId').value && this.form.get('projectId').value) {
      this.parentTask$ = this.taskService
        .get(this.form.get('projectId').value!, this.form.get('parentTaskId').value!)
        .pipe(
          tap((parentTask) => {
            this.taskTypes$ = this.taskService.listTaskTypes(parentTask.type);
          }),
        );
      this.parentTask$.subscribe();
    }
  }

  onProjectIdChange(projectId: number | null) {
    if (!projectId) return;
    console.log('projectId change', projectId);

    this.statusList$ = this.statusService.list(projectId);

    this.statusList$
      .pipe(untilDestroyed(this), withLoader(this.statusListLoader))
      .subscribe((statuses) => {
        const statusId = statuses.find((status) => status.begin)?.id;

        if (!statusId) return;

        this.form.patchValue({ statusId });
      });

    this.gitIntegrationService.hasGitHubIntegration(projectId).subscribe((value) => {
      this.isGitHubIntegrationAvailable = value;

      if (value) {
        this.issueList$ = this.gitIntegrationService.gitHubIssueList(projectId);
        this.pullList$ = this.gitIntegrationService.gitHubPullList(projectId);
      }
    });
  }

  confirm() {
    if (validateForm(this.form)) {
      this.dialogRef.close(this.form.value);
    }
  }

  cancel() {
    this.dialogRef.close(false);
  }
}
