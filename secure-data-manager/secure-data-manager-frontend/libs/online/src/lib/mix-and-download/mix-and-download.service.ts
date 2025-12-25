/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {BallotBox} from '@sdm/shared-util-types';
import {Observable} from 'rxjs';
import {environment} from "@sdm/shared-ui-config";

@Injectable({
	providedIn: 'root',
})
export class MixAndDownloadService {
	private readonly url = `${environment.backendPath}/sdm-online/mix-download`;

	constructor(private readonly httpClient: HttpClient) {
	}

	mixAndDownload(ballotBoxes: BallotBox[]): void {
		const ballotBoxIds = ballotBoxes.map((ballotBox) => ballotBox.id);
		this.httpClient.post(this.url, ballotBoxIds).subscribe();
	}

	getBallotBoxes(): Observable<BallotBox[]> {
		return this.httpClient.get<BallotBox[]>(`${this.url}/ballotboxes`);
	}
}
