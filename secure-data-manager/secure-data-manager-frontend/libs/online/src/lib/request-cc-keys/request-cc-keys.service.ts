/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from "@sdm/shared-ui-config";

@Injectable({
	providedIn: 'root',
})
export class RequestCcKeysService {
	private readonly url = `${environment.backendPath}/sdm-online/request-cc-keys`;

	constructor(private readonly httpClient: HttpClient) {
	}

	requestCcKeys(): void {
		this.httpClient.post(this.url, null).subscribe();
	}
}
