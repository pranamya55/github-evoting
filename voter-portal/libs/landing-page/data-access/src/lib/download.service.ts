/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {inject, Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {TranslateService} from "@ngx-translate/core";
import {APP_ENVIRONMENT, Environment} from "@vp/landing-page-utils-types";

@Injectable({
	providedIn: 'root',
})
export class DownloadService {
	private readonly http: HttpClient = inject(HttpClient);
	private readonly translateService = inject(TranslateService);
	private readonly environment: Environment = inject(APP_ENVIRONMENT);

	download(captchaToken: string) {
		if (!this.environment.captcha){
			console.error("Captcha configuration is not available in the environment.");
			return;
		}
		const params = {
			lang: this.translateService.currentLang,
			[this.environment.captcha.responseParameterName]: captchaToken!
		};

		this.http.get(this.environment.captcha.url, {
			params,
			observe: 'response',
			responseType: 'blob'
		}).subscribe(resp => {
			const contentDisposition = resp.headers.get('Content-Disposition');
			const match = contentDisposition?.match(/filename="?([^"]+)"?/);
			const filename = match?.[1] ?? 'voting-card.pdf';

			const objectUrl = URL.createObjectURL(resp.body!);
			const a = document.createElement('a');
			a.href = objectUrl;
			a.download = filename;
			a.click();
			URL.revokeObjectURL(objectUrl);
		});
	}
}