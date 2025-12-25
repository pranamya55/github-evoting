/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {API_BASE_PATH, EvotingComponent, EvotingComponents, Phase} from '../app.module';
import {first, Observable, of, retry, shareReplay, switchMap, tap} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SessionService {

  private phase: Phase | null;
  private component: EvotingComponent[] | undefined;
  private _sessionIdObservable: Observable<string>;


  constructor(private http: HttpClient) {
    this.phase = null;
    this._sessionIdObservable = this.createSessionObserver();
    this.update();
  }

  get currentPhase() {
    return this.phase;
  }

  get currentComponents() {
    return this.component;
  }

  public getSession(): Observable<string> {
    return this._sessionIdObservable.pipe(
      first()
    );
  }

  public update() {
    // update the phase
    this.getSession().pipe(
      switchMap(sessionId => this.http.get(`${API_BASE_PATH}/session/${sessionId}`).pipe(retry({delay: 1000}))),
    ).subscribe(phase => {
        this.phase = Phase[phase as keyof typeof Phase];
      }
    );

    // update the component selected
    this.getSession().pipe(
      switchMap(sessionId => this.http.get(`${API_BASE_PATH}/session/${sessionId}/selected`).pipe(retry({delay: 1000})))
    ).subscribe(value => {
      const availableComponents = <string[]>value;
      this.component = availableComponents
        .map(component => EvotingComponents[component])
        .sort((a, b) => a.label.localeCompare(b.label));
    });
  }

  reset() {
    this._sessionIdObservable.pipe(
      switchMap(sessionId => this.http.delete(`${API_BASE_PATH}/session/${sessionId}`))
    ).subscribe(value => {
      localStorage.removeItem("sessionId");
      this.phase = null;
      this._sessionIdObservable = this.createSessionObserver();
      this.update();
    });
  }

  private createSessionObserver() {
    return of(localStorage.getItem("sessionId") || '').pipe(
      switchMap(sessionId => {
        if (sessionId) {
          return of(sessionId).pipe(first());
        } else {
          return this.http.post(`${API_BASE_PATH}/session`, {}, {responseType: 'text'}).pipe(
            retry({delay: 1000}),
            tap((sessionId: string) => {
              localStorage.setItem("sessionId", sessionId);
            })
          );
        }
      }),
      shareReplay(1)
    );
  }
}
