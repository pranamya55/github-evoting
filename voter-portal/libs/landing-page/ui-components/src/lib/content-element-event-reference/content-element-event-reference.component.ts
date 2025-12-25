/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ActivatedRoute} from "@angular/router";
import {TenantConfigurationService} from "@vp/landing-page-data-access";
import {combineLatest, Subject, takeUntil} from "rxjs";
import {Component, inject, Input, OnDestroy, OnInit} from '@angular/core';
import {EventReferenceContentElement, EventElement, EventPhaseElement, TenantStateEvent} from "@vp/landing-page-utils-types";

@Component({
	selector: 'vp-content-element-event-reference',
	standalone: true,
	templateUrl: './content-element-event-reference.component.html'
})
export class ContentElementEventReferenceComponent implements OnInit, OnDestroy {
	@Input({required: true}) element!: EventReferenceContentElement;

	private readonly configurationService: TenantConfigurationService = inject(TenantConfigurationService);
	private readonly phaseParamRegex = /^([a-zA-Z0-9_-]{1,20},(?:UPCOMING|OPEN|EVOTING-CLOSED|CLOSED))(\|[a-zA-Z0-9_-]{1,20},(?:UPCOMING|OPEN|EVOTING-CLOSED|CLOSED))*$/;
	private readonly route = inject(ActivatedRoute);

	private readonly destroy$ = new Subject<void>();

	ngOnInit(): void {
		combineLatest([
			this.configurationService.getEvents(),
			this.configurationService.getStates(),
			this.route.queryParamMap
		])
			.pipe(takeUntil(this.destroy$))
			.subscribe(([events, states, params]) => {
				const phaseEventElement = this.applyPhaseFiltering(events, states, params.get('phases'));
				this.element.contentElements = phaseEventElement ? phaseEventElement.contentElements : [];
			});
	}

	ngOnDestroy(): void {
		this.destroy$.next();
		this.destroy$.complete();
	}

	private applyPhaseFiltering(events: EventElement[], states: TenantStateEvent[], phasesParam: string | null): EventPhaseElement {
		const event = events.find(e => e.id === this.element.id);
		if (!event) {
			return undefined as unknown as EventPhaseElement;
		}

		// Check for phase override in URL parameter
		if (phasesParam && this.phaseParamRegex.test(phasesParam)) {
			const phaseInfo = phasesParam
				.split("|")
				.map(info => info.split(","))
				.find(([eventId]) => eventId === this.element.id);

			if (phaseInfo) {
				const [, phaseLevel] = phaseInfo;
				const phase = event.phases.find(p => p.phaseLevel === phaseLevel);
				if (phase) {
					return phase as EventPhaseElement;
				}
			}
		}

		// Fallback to active phase
		const activePhaseLevel = states.find(s => s.id === this.element.id)?.activePhase;
		const activePhase = event.phases.find(phase => phase.phaseLevel === activePhaseLevel);
		return activePhase as EventPhaseElement;
	}

}