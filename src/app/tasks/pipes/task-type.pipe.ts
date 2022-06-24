import { Pipe, PipeTransform } from '@angular/core';
import { TaskType } from '@tasks/enums/task-type.enum';
import * as Color from 'color';

@Pipe({
  name: 'taskType',
})
export class TaskTypePipe implements PipeTransform {
  transform(value: any, type?: 'name' | 'icon'): string;
  transform(value: any, type: 'color'): Color;

  transform(value: any, type: 'color' | 'name' | 'icon' = 'name'): string | Color {
    if (typeof value === 'number') value = TaskType[value];

    switch (value) {
      // Task
      case 'TASK':
        return {
          name: $localize`Task`,
          color: Color.rgb(52, 152, 219),
          icon: 'cuTask',
        }[type];

      // User story
      case 'USER_STORY':
        return {
          name: $localize`User story`,
          color: Color.rgb(46, 204, 113),
          icon: 'cuUserStory',
        }[type];

      // Issue
      case 'ISSUE':
        return {
          name: $localize`Issue`,
          color: Color.rgb(231, 76, 60),
          icon: 'cuIssue',
        }[type];

      // Epic
      case 'EPIC':
        return {
          name: $localize`Epic`,
          color: Color.rgb(155, 89, 182),
          icon: 'cuEpic',
        }[type];

      // Subtask
      case 'SUBTASK':
        return {
          name: $localize`Subtask`,
          color: Color.rgb(88, 183, 189),
          icon: 'cuSubtask',
        }[type];

      // Unknown
      default:
        console.warn('Unknown task type:', value);
        return value;
    }
  }
}
