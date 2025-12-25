/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */


import {Component, ViewChild} from '@angular/core';
import {ActivatedRouteSnapshot, RouterOutlet} from '@angular/router';
import {HeaderComponent} from '../header/header.component';
import {StepperComponent} from '../stepper/stepper.component';
import {environment} from "@sdm/shared-ui-config";

@Component({
	selector: 'sdm-root',
	standalone: true,
	imports: [
		HeaderComponent,
		RouterOutlet,
		StepperComponent
	],
	templateUrl: './app.component.html',
})
export class AppComponent {
	@ViewChild('routerOutlet', {read: RouterOutlet}) routerOutlet!: RouterOutlet;
	activatedRouteSnapshot?: ActivatedRouteSnapshot;
	workflowEnabled?: boolean;

	constructor() {
		this.workflowEnabled = environment.workflowEnabled;
	}

	registerRoute() {
		this.activatedRouteSnapshot = this.routerOutlet.activatedRoute.snapshot;
	}
}
