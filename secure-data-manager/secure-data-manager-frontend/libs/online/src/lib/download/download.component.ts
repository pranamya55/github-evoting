/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {DownloadService} from './download.service';

@Component({
	selector: 'sdm-download',
	standalone: true,
	imports: [
		CommonModule,
		TranslateModule,
		ProgressComponent,
		PageActionsComponent,
		NextButtonComponent,
		PageTitleComponent,
	],
	templateUrl: './download.component.html',
})
export class DownloadComponent {
	constructor(private readonly downloadService: DownloadService) {
	}

	download() {
		this.downloadService.download();
	}
}
