/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Injectable, NgZone } from '@angular/core';
import { EMPTY, Observable, share, Subscriber } from 'rxjs';
import { VotingServerHealth } from '@sdm/shared-util-types';
import { environment } from '@sdm/shared-ui-config';

@Injectable({
  providedIn: 'root',
})
export class VotingServerHealthService {
  private readonly votingServerHealth: Observable<VotingServerHealth>;

  constructor(private readonly ngZone: NgZone) {
    this.votingServerHealth = environment.remoteServerAvailable
      ? this.getVotingServerHealth().pipe(share())
      : EMPTY;
  }

  get(): Observable<VotingServerHealth> {
    return this.votingServerHealth;
  }

  private getVotingServerHealth(): Observable<VotingServerHealth> {
    return new Observable((subscriber: Subscriber<VotingServerHealth>) => {
      const eventSource = new EventSource(
        `${environment.backendPath}/sdm-online/voting-server-health/subscribe`,
      );

      eventSource.onmessage = (message: MessageEvent<string>) => {
        this.ngZone.run(() => {
          subscriber.next(JSON.parse(message.data));
        });
      };

      eventSource.onerror = (error) => {
        this.ngZone.run(() => {
          subscriber.error(error);
        });
      };
    });
  }
}
