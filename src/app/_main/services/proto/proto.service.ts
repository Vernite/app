import { Injectable } from '@angular/core';
import { webSocket } from 'rxjs/webSocket';
import {
  filter,
  from,
  map,
  of,
  switchMap,
  Observable,
  OperatorFunction,
  share,
  finalize,
} from 'rxjs';
import { Message } from 'google-protobuf';
import { Any } from 'google-protobuf/google/protobuf/any_pb.js';
import { Service } from '../../decorators/service/service.decorator';
import { vernite } from '@vernite/protobuf';
import { isClass } from '@main/classes/util/is-class';
import { environment } from '../../../../environments/environment';

@Service()
@Injectable({
  providedIn: 'root',
})
export class ProtoService {
  protected _websocket$ = webSocket({
    url: environment.websocketUrl,
    deserializer: (e: MessageEvent) => e,
    serializer: (value: any) => value,
  });

  private messageClasses = this.flatClassesMap('vernite', vernite);
  private packagesMap = this.flatPackagesMap('vernite', vernite);

  constructor() {
    this.subscribe<vernite.KeepAlive, typeof vernite.KeepAlive>(vernite.KeepAlive).subscribe(
      (message) => {
        this.next(message);
      },
    );
  }

  protected isType<T extends Message, K extends typeof Message>(
    cls: K,
  ): OperatorFunction<Message, T> {
    return filter((message: T) => {
      return message instanceof cls;
    }) as OperatorFunction<Message, T>;
  }

  private serialize(value: Message) {
    const any = new Any();
    any.pack(value.serializeBinary(), this.packagesMap.get(value.constructor.name)!);
    return of(any.serializeBinary());
  }

  private deserialize(e: MessageEvent) {
    return from(e.data.arrayBuffer() as Promise<Uint8Array>).pipe(
      map((buffer: Uint8Array) => {
        const any = Any.deserializeBinary(buffer);
        const type = any.getTypeName();

        const messageClasses = this.messageClasses;
        const message = any.unpack((bytes: Uint8Array) => {
          return messageClasses.get(type)!.deserializeBinary(bytes);
        }, type);

        return message!;
      }),
    );
  }

  public next(value: Message) {
    this.serialize(value).subscribe((data) => {
      this._websocket$.next(data as any);
    });
  }

  protected socket() {
    return this._websocket$.pipe(switchMap(this.deserialize.bind(this)));
  }

  public subscribe<T extends Message & { action?: vernite.BasicAction }, K extends typeof Message>(
    cls: K,
    action?: vernite.BasicAction,
  ): Observable<T> {
    return this.socket().pipe(
      this.isType<T, K>(cls),
      filter((message: T) => {
        if (!action) return true;
        return message.action === action;
      }),
      finalize(() => {
        this.unsubscribe();
      }),
      share(),
    );
  }

  protected unsubscribe() {
    console.groupCollapsed('[SOCKET] UNSUBSCRIBE');
    console.log('Closing socket');
    console.groupEnd();
  }

  private flatClassesMap(prefix: string, source: any): Map<string, typeof Message> {
    let map = new Map<string, typeof Message>();
    for (const [key, cls] of Object.entries(source)) {
      if (isClass<typeof Message>(cls)) {
        map.set(prefix + '.' + key, cls);
        map = new Map([
          ...map.entries(),
          ...this.flatClassesMap(prefix + '.' + key, cls).entries(),
        ]);
      }
    }

    return map;
  }

  private flatPackagesMap(prefix: string, source: any): Map<string, string> {
    let map = new Map<string, string>();
    for (const [key, cls] of Object.entries(source)) {
      if (isClass<typeof Message>(cls)) {
        map.set(key, prefix + '.' + key);
        map = new Map([
          ...map.entries(),
          ...this.flatPackagesMap(prefix + '.' + key, cls).entries(),
        ]);
      }
    }

    return map;
  }
}
