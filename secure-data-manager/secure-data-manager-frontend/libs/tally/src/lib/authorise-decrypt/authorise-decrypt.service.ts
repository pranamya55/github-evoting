/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {BoardMember} from '@sdm/shared-util-types';
import {Observable} from 'rxjs';
import {HttpClient} from "@angular/common/http";
import {environment} from "@sdm/shared-ui-config";

@Injectable({
	providedIn: 'root',
})
export class AuthoriseDecryptService {
	url = `${environment.backendPath}/sdm-tally/validate-electoral-board`;

	constructor(private readonly httpClient: HttpClient) {
	}

	getElectoralBoardMembers(): Observable<BoardMember[]> {
		return this.httpClient.get<BoardMember[]>(this.url);
	}

	validatePassword(
		boardMember: BoardMember,
		password: string,
	): Observable<boolean> {
		return this.httpClient.put<boolean>(`${this.url}/${boardMember.id}`, [...password]);
	}
}
