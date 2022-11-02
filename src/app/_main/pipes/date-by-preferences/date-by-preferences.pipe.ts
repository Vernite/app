import { PipeTransform } from '@angular/core';
import dayjs from 'dayjs';
import { Pipe } from '@angular/core';
import { UserService } from '@auth/services/user/user.service';
import { EMPTY, map, tap } from 'rxjs';

@Pipe({
  name: 'dateByPreferences',
})
export class DateByPreferencesPipe implements PipeTransform {
  constructor(private userService: UserService) {}

  transform(value: any): any {
    if (value) {
      return this.userService.getDateFormat().pipe(
        tap((dateFormat: string) => console.log([value, dateFormat])),
        map((dateFormat: string) => dayjs(value).format(dateFormat)),
      );
    } else {
      return EMPTY;
    }
  }
}
