/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {ComputeService} from './compute.service';

@Component({
	selector: 'sdm-compute',
	standalone: true,
	imports: [
		CommonModule,
		TranslateModule,
		PageActionsComponent,
		ProgressComponent,
		NextButtonComponent,
		PageTitleComponent,
	],
	templateUrl: './compute.component.html',
})
export class ComputeComponent {
	constructor(private readonly computeService: ComputeService) {
	}

	compute() {
		this.computeService.compute();
	}
}
