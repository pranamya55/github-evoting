/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';

import {RouterLink} from "@angular/router";
import {RoutingService} from "@sdm/shared-ui-services";
import {TranslateModule} from "@ngx-translate/core";

@Component({
	selector: 'sdm-next-button',
	standalone: true,
	imports: [TranslateModule, RouterLink],
	templateUrl: './next-button.component.html',
})
export class NextButtonComponent {
	@Input() disabled = false;
	linkToNextStep: string[] | null;

	constructor(
		readonly routingService: RoutingService,
	) {
		this.linkToNextStep = this.routingService.getLinkToNextStep();
	}
}
