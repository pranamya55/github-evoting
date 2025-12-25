/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from "@sdm/shared-ui-config";
import {Observable} from "rxjs";
import {VoterPortalSdmConfig} from "@sdm/shared-util-types";

@Injectable({
	providedIn: 'root',
})
export class ConfigureVoterPortalService {
	private readonly url = `${environment.backendPath}/sdm-online/configure-voter-portal`;

	constructor(private readonly httpClient: HttpClient) {
	}

	configureVoterPortal(): void {
		this.httpClient.post(this.url, {}).subscribe();
	}

	getConfiguration(): Observable<VoterPortalSdmConfig> {
		return this.httpClient.get<VoterPortalSdmConfig>(this.url);
	}
}
