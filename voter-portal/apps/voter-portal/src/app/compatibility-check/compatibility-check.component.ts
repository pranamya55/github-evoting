/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, inject} from '@angular/core';
import {BackendService} from "@vp/voter-portal-data-access";
import {from, Observable} from "rxjs";
import {map, take} from "rxjs/operators";

@Component({
	selector: 'vp-compatibility-check',
	templateUrl: './compatibility-check.component.html',
	standalone: false
})
export class CompatibilityCheckComponent {
	private readonly backendService = inject(BackendService);

	isBrowserCompatible$: Observable<boolean>;

	constructor() {
		this.isBrowserCompatible$ = from(this.backendService.isBrowserCompatible()).pipe(
			take(1),
			map(isBrowserCompatible => isBrowserCompatible)
		);
	}
}
