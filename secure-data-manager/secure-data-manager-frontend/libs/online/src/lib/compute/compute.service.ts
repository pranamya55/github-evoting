/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from "@sdm/shared-ui-config";

@Injectable({
	providedIn: 'root',
})
export class ComputeService {
	private readonly url = `${environment.backendPath}/sdm-online/compute`;

	constructor(private readonly httpClient: HttpClient) {
	}

	compute(): void {
		this.httpClient.post(this.url, {}).subscribe();
	}
}
