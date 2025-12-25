/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {ExportInfo} from '@sdm/shared-util-types';
import {Observable} from 'rxjs';
import {environment} from '@sdm/shared-ui-config';

@Injectable({
	providedIn: 'root',
})
export class DataExchangeService {
	private readonly url = `${environment.backendPath}/sdm-shared/data-exchange`;
	private readonly exportUrl = `${this.url}/export`;
	private readonly importUrl = `${this.url}/import`;

	constructor(private readonly httpClient: HttpClient) {
	}

	getExportInfo(exchangeIndex: string): Observable<ExportInfo> {
		const options = exchangeIndex
			? {params: new HttpParams().set('exchangeIndex', exchangeIndex)}
			: {};

		return this.httpClient.get<ExportInfo>(this.exportUrl, options);
	}

	export(exchangeIndex: string): Observable<unknown> {
		return this.httpClient.post(this.exportUrl, parseInt(exchangeIndex, 10));
	}

	import(sdmZipFile: File, exchangeIndex: string): Observable<any> {
		const formData = new FormData();
		formData.append('file', sdmZipFile);
		formData.append('exchangeIndex', exchangeIndex);
		return this.httpClient.post(this.importUrl, formData);
	}
}
