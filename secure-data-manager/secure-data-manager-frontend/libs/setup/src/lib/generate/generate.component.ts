/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Component} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {GenerateService} from './generate.service';

@Component({
	selector: 'sdm-generate',
	standalone: true,
	imports: [
		TranslateModule,
		ProgressComponent,
		PageActionsComponent,
		NextButtonComponent,
		PageTitleComponent
	],
	templateUrl: './generate.component.html',
})
export class GenerateComponent {
	constructor(private readonly generateService: GenerateService) {
	}

	generate() {
		this.generateService.generate();
	}
}
