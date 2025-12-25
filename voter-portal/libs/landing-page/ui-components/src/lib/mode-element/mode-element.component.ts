/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TenantConfigurationService} from "@vp/landing-page-data-access";
import {Subject} from "rxjs";
import {Component, inject} from '@angular/core';
import {ModeElement} from "@vp/landing-page-utils-types";
import {ContentElementComponent} from "../content-element/content-element.component";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
	selector: 'vp-mode-element',
	standalone: true,
	imports: [
		ContentElementComponent
	],
	templateUrl: './mode-element.component.html'
})
export class ModeElementComponent {
	element!: ModeElement

	private readonly configurationService: TenantConfigurationService = inject(TenantConfigurationService);
	private readonly destroy$ = new Subject<void>();

	constructor() {
		this.configurationService.getMode()
			.pipe(takeUntilDestroyed())
			.subscribe((mode) => {
				this.element = mode ? mode : {} as ModeElement;
			});
	}
}