/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {DatasetInfo, TallyFileInfo} from '@sdm/shared-util-types';
import {DataCollectionService} from './data-collection.service';
import {ProgressComponent} from "@sdm/shared-feature-progress";

@Component({
	selector: 'sdm-data-collection',
	standalone: true,
	imports: [
		CommonModule,
		TranslateModule,
		PageActionsComponent,
		ProgressComponent,
		NextButtonComponent,
		PageTitleComponent,
	],
	templateUrl: './data-collection.component.html',
})
export class DataCollectionComponent {
	mode = '';
	datasetInfo?: DatasetInfo;
	tallyFileInfo?: TallyFileInfo;

	constructor(
		private readonly dataCollectionService: DataCollectionService,
		private readonly route: ActivatedRoute,
	) {
		this.mode = this.route.snapshot.data['mode'];

		this.dataCollectionService
			.getDatasetFilenameList(this.mode)
			.subscribe((datasetInfo) => {
				this.datasetInfo = datasetInfo;
			});

		if (this.mode === 'tally') {
			this.dataCollectionService
				.getTallyFileInfo()
				.subscribe((tallyFileInfo) => {
					this.tallyFileInfo = tallyFileInfo;
				});
		}

	}

	collect() {
		this.dataCollectionService.collect(this.mode);
	}

	get datasetFilenameList(): string[] {
		if (!this.datasetInfo) return [];
		return this.datasetInfo.filenames;
	}

	get tallyFilenameList(): string[] {
		return this.tallyFileInfo?.filenames ?? [];
	}

}
