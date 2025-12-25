/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Component} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {PreComputeService} from './pre-compute.service';

@Component({
	selector: 'sdm-pre-compute',
	standalone: true,
	imports: [
		TranslateModule,
		ProgressComponent,
		PageActionsComponent,
		NextButtonComponent,
		PageTitleComponent
	],
	templateUrl: './pre-compute.component.html',
})
export class PreComputeComponent {
	constructor(private readonly preComputeService: PreComputeService) {
	}

	preCompute() {
		this.preComputeService.preComputeVerificationCardSets();
	}
}
