/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule, DatePipe} from '@angular/common';
import {Component, DestroyRef} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {GeneratePrintFileService} from './generate-print-file.service';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {PrintInfo} from '@sdm/shared-util-types';

@Component({
	selector: 'sdm-generate-print-file',
	standalone: true,
	providers: [DatePipe],
	imports: [
		CommonModule,
		TranslateModule,
		PageActionsComponent,
		ProgressComponent,
		NextButtonComponent,
		PageTitleComponent,
	],
	templateUrl: './generate-print-file.component.html',
})
export class GeneratePrintFileComponent {
	printInfo?: PrintInfo;

	constructor(
		private readonly generateService: GeneratePrintFileService,
		private readonly destroyRef: DestroyRef,
	) {
		this.generateService.getPrintInfo().subscribe((printInfo) => {
			this.printInfo = printInfo;
		});
	}

	generatePrintFile() {
		this.generateService
			.generatePrintFile()
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe();
	}
}
