/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {HttpClient} from "@angular/common/http";
import {environment} from "@sdm/shared-ui-config";
import {BallotBoxResults} from "e-voting-libraries-ui-kit";

@Injectable({
	providedIn: 'root'
})
export class ResultsService {
	private readonly url = `${environment.backendPath}/sdm-tally/decrypt/ballotboxresults`;

	private readonly httpClient = inject(HttpClient);

	getBallotBoxResults(ballotBoxId: string): Observable<BallotBoxResults> {
		return this.httpClient.get<BallotBoxResults>(`${this.url}/${ballotBoxId}`);
	}
}
