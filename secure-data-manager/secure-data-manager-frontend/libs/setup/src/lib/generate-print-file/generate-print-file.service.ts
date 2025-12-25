/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '@sdm/shared-ui-config';
import {Observable} from 'rxjs';
import {PrintInfo} from '@sdm/shared-util-types';

@Injectable({
	providedIn: 'root',
})
export class GeneratePrintFileService {
	private readonly url = `${environment.backendPath}/sdm-setup/generate-print-file`;

	constructor(private readonly http: HttpClient) {
	}

	generatePrintFile() {
		return this.http.post(this.url, null);
	}

	getPrintInfo(): Observable<PrintInfo> {
		return this.http.get<PrintInfo>(this.url);
	}
}
