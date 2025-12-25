/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {environment} from '@sdm/shared-ui-config';
import {Summary} from "e-voting-libraries-ui-kit";

@Injectable({
	providedIn: 'root',
})
export class PreConfigureService {
	private readonly url = `${environment.backendPath}/sdm-setup/pre-configure`;

	constructor(private readonly httpClient: HttpClient) {
	}

	previewSummary(): Observable<Summary> {
		return this.httpClient.get<Summary>(`${environment.backendPath}/sdm-setup/pre-configure/preview`);
	}

	preConfigureElectionEvent(): void {
		this.httpClient.post(this.url, null).subscribe();
	}
}
