/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {RequestCcKeysService} from './request-cc-keys.service';
import {ProgressComponent} from "@sdm/shared-feature-progress";

@Component({
	selector: 'sdm-request-cc-keys',
	standalone: true,
	imports: [
		CommonModule,
		TranslateModule,
		PageActionsComponent,
		ProgressComponent,
		NextButtonComponent,
		PageTitleComponent,
	],
	templateUrl: './request-cc-keys.component.html',
})
export class RequestCcKeysComponent {
	constructor(private readonly requestCcKeysService: RequestCcKeysService) {
	}

	requestCcKeys() {
		this.requestCcKeysService.requestCcKeys();
	}
}
