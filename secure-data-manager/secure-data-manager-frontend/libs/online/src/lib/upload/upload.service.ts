/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '@sdm/shared-ui-config';
import {Observable} from 'rxjs';

@Injectable({
	providedIn: 'root',
})
export class UploadService {
	private readonly url = `${environment.backendPath}/sdm-online/upload`;

	constructor(private readonly httpClient: HttpClient) {
	}

	upload(day: number): Observable<Object> {
		return this.httpClient.post(this.url, day);
	}
}
