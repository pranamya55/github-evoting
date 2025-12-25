/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {CommonModule} from '@angular/common';
import {Component, Inject, Input} from '@angular/core';
import {APP_ROUTES, SdmRoute} from '@sdm/shared-util-types';
import {StepperItemComponent} from '../stepper-item/stepper-item.component';
import {ActivatedRouteSnapshot} from '@angular/router';

@Component({
	selector: 'sdm-stepper',
	standalone: true,
	imports: [CommonModule, StepperItemComponent],
	templateUrl: './stepper.component.html',
})
export class StepperComponent {
	@Input({required: true}) activatedRouteSnapshot?: ActivatedRouteSnapshot;
	groupedRoutes = new Map<SdmRoute, SdmRoute[]>();

	constructor(@Inject(APP_ROUTES) private readonly routes: SdmRoute[]) {
		this.routes.forEach((route) => {
			if (route.children) {
				const groupRoutes = this.groupedRoutes.get(route) ?? [];
				this.groupedRoutes.set(route, [...groupRoutes, ...route.children]);
			}
		});
	}
}
