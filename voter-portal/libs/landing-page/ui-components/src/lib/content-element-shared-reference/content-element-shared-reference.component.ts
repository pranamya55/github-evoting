/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TenantConfigurationService} from "@vp/landing-page-data-access";
import {combineLatest, Subject, takeUntil} from "rxjs";
import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {SharedReferenceContentElement} from "@vp/landing-page-utils-types";

@Component({
	selector: 'vp-content-element-shared-reference',
	standalone: true,
	templateUrl: './content-element-shared-reference.component.html'
})
export class ContentElementSharedReferenceComponent implements OnInit, OnDestroy {
	@Input({required: true}) element!: SharedReferenceContentElement;

	private readonly configurationService: TenantConfigurationService = inject(TenantConfigurationService);
	private readonly destroy$ = new Subject<void>();

	ngOnInit(): void {
		combineLatest([
			this.configurationService.getShared()
		])
			.pipe(takeUntil(this.destroy$))
			.subscribe(([shared]) => {
				const sharedElement = shared.find(e => e.id === this.element.id);
				this.element.contentElements = sharedElement ? sharedElement.contentElements : [];
			});
	}

	ngOnDestroy(): void {
		this.destroy$.next();
		this.destroy$.complete();
	}
}