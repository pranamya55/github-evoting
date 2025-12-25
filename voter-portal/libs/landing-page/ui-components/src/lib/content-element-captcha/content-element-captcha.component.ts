/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {IconComponent} from "@vp/shared-ui-components";
import {DownloadService} from "@vp/landing-page-data-access";
import {TranslateTextPipe} from "e-voting-libraries-ui-kit";
import {CaptchaContentElement} from "@vp/landing-page-utils-types";
import {Component, inject, Input, ViewChild} from '@angular/core';
import {NgHcaptchaComponent, NgHcaptchaModule} from "ng-hcaptcha";

@Component({
	selector: 'vp-content-element-captcha',
	standalone: true,
	imports: [
		NgHcaptchaModule,
		TranslateTextPipe,
		IconComponent
	],
	templateUrl: './content-element-captcha.component.html'
})
export class ContentElementCaptchaComponent {
	@Input({required: true}) element!: CaptchaContentElement;
	@ViewChild(NgHcaptchaComponent)
	hcaptchaComponent!: NgHcaptchaComponent;

	private readonly downloadService: DownloadService = inject(DownloadService);
	captchaToken: string | undefined = undefined;

	onVerify(token: string) {
		this.captchaToken = token;
	}

	onExpired(response: any) {
		console.warn('Captcha expired:', response);
		this.captchaToken = undefined;
	}

	onError(error: any) {
		console.error('Captcha error:', error);
		this.captchaToken = undefined;
	}

	download() {
		this.downloadService.download(this.captchaToken!);
		this.hcaptchaComponent.reset();
		this.captchaToken = undefined;
	}
}
