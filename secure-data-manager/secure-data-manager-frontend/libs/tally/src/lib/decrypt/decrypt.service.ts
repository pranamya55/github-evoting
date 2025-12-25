/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {BallotBox, DecryptInput} from '@sdm/shared-util-types';
import {Observable} from 'rxjs';
import {HttpClient} from "@angular/common/http";
import {environment} from "@sdm/shared-ui-config";

@Injectable({
	providedIn: 'root'
})
export class DecryptService {
	private readonly url = `${environment.backendPath}/sdm-tally/decrypt`;

	constructor(private readonly httpClient: HttpClient) {
	}

	decrypt(ballotBoxes: BallotBox[], electoralBoardPasswords: string[]) {
		const ballotBoxIds = ballotBoxes.map((ballotBox) => ballotBox.id);
		const passwordsAsCharArray: string[][] = electoralBoardPasswords.map((pwd) => [...pwd]);
		const values: DecryptInput = {
			ballotBoxIds: ballotBoxIds,
			electoralBoardPasswords: passwordsAsCharArray
		}
		return this.httpClient.post(this.url, values).subscribe();
	}

	getBallotBoxes(): Observable<BallotBox[]> {
		return this.httpClient.get<BallotBox[]>(`${this.url}/ballotboxes`);
	}
}
