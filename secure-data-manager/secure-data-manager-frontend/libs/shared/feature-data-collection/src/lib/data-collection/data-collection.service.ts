/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from "rxjs";
import {DatasetInfo} from "@sdm/shared-util-types";
import {environment} from '@sdm/shared-ui-config';
import {TallyFileInfo} from "../../../../util-types/src/lib/tally-file-info";

@Injectable({
	providedIn: 'root',
})
export class DataCollectionService {
	private readonly urlSuffix = `collect-data-verifier`;

	constructor(private readonly httpClient: HttpClient) {
	}

	getDatasetFilenameList(mode: string): Observable<DatasetInfo> {
		return this.httpClient.get<DatasetInfo>(`${environment.backendPath}/sdm-${mode}/${this.urlSuffix}/dataset-info`);
	}

	collect(mode: string): void {
		this.httpClient.post(`${environment.backendPath}/sdm-${mode}/${this.urlSuffix}`, null).subscribe();
	}

	getTallyFileInfo(): Observable<TallyFileInfo> {
		return this.httpClient.get<TallyFileInfo>(`${environment.backendPath}/sdm-tally/${this.urlSuffix}/tally-file-info`);
	}
}
