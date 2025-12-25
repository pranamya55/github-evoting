/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {UploadService} from './upload.service';

@Component({
	selector: 'sdm-upload',
	standalone: true,
	imports: [
		CommonModule,
		TranslateModule,
		ProgressComponent,
		PageActionsComponent,
		NextButtonComponent,
		PageTitleComponent,
	],
	templateUrl: './upload.component.html',
})
export class UploadComponent {
	day: number = 1;

	constructor(
		readonly route: ActivatedRoute,
		private readonly uploadService: UploadService,
	) {
		this.day = this.route.snapshot.data['day'];
	}

	upload(): void {
		this.uploadService.upload(this.day).subscribe();
	}
}
