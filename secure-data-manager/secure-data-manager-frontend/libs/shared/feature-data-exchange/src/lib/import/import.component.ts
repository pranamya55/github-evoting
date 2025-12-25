/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {DataExchangeService} from '../data-exchange.service';
import {ProgressComponent} from '@sdm/shared-feature-progress';

@Component({
	selector: 'sdm-import',
	standalone: true,
	imports: [
		CommonModule,
		FormsModule,
		TranslateModule,
		PageActionsComponent,
		ProgressComponent,
		NextButtonComponent,
		PageTitleComponent,
	],
	templateUrl: './import.component.html',
})
export class ImportComponent {
	exchangeIndex = '';
	sdmZipToUpload: File[] = [];
	forceProgress = false;

	constructor(
		private readonly dataExchangeService: DataExchangeService,
		private readonly route: ActivatedRoute,
	) {
		this.exchangeIndex = this.route.snapshot.data['exchangeIndex'];
	}

	onFileSelected(event: Event) {
		const target = event.target as HTMLInputElement;
		if (target.files && target.files.length > 0) {
			const file = target.files[0];
			this.sdmZipToUpload = [file];
			// Reset the value in case of selecting the same file again (so onChange is triggered).
			target.value = '';
			this.import();
		}
	}

	import() {
		this.forceProgress = true;
		this.dataExchangeService
			.import(this.sdmZipToUpload[0], this.exchangeIndex)
			.subscribe(_ => this.forceProgress = false);
	}
}
