import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { ApiService } from 'src/app/_main/services/api.service';
import { Workspace } from '../interfaces/workspace.interface';

/**
 * Workspaces management service
 */
@Injectable({
  providedIn: 'root',
})
export class WorkspaceService {
  list$ = new Subject<Workspace[]>();

  /**
   * Default constructor with `ApiService` dependency.
   * @param apiService Api service
   */
  constructor(private apiService: ApiService) {}

  /**
   * Gets a workspace by its ID.
   * @param id identifier of the workspace to get from the API
   * @returns Request observable, which completes when request is finished
   */
  public get(id: number): Observable<Workspace> {
    return this.apiService.get(`/workspace/${id}`);
  }

  /**
   * Deletes the workspace by its ID.
   * @param id identifier of the workspace to delete from the API
   * @returns Request observable, which completes when request is finished
   */
  public delete(id: number): Observable<null> {
    return this.apiService.delete(`/workspace/${id}`);
  }

  /**
   * Updates a workspace.
   * @param workspace workspace object to update in the API
   * @returns Request observable, which completes when request is finished
   */
  public update(workspace: Workspace): Observable<Workspace> {
    return this.apiService.put(`/workspace/${workspace.id}`, { body: workspace });
  }

  /**
   * Lists all available workspaces.
   * @returns Request observable, which completes when request is finished
   */
  public list(): Observable<Workspace[]> {
    return this.apiService.get(`/workspace`);
  }

  /**
   * Creates a new workspace.
   * @param workspace workspace to modify
   * @returns Request observable, which completes when request is finished
   */
  public create(workspace: Workspace): Observable<Workspace> {
    return this.apiService.post(`/workspace`, { body: workspace });
  }
}
