/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {BoardMember} from '@sdm/shared-util-types';
import {environment} from "@sdm/shared-ui-config";

@Injectable({
	providedIn: 'root',
})
export class ConstituteElectoralBoardService {
	private readonly url = `${environment.backendPath}/sdm-setup/constitute-electoral-board`;

	constructor(private readonly httpClient: HttpClient) {
	}

	constitute(membersPasswords: string[]): void {
		const passwords: string[][] = [];
		membersPasswords.forEach((pwd) => passwords.push([...pwd]));
		this.httpClient.post(this.url, passwords).subscribe();
	}

	getMembers(): Observable<BoardMember[]> {
		return this.httpClient.get<BoardMember[]>(this.url);
	}
}
