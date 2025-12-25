/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Inject, inject, Injectable} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {APP_ROUTES, SdmRoute, WorkflowStep} from '@sdm/shared-util-types';

@Injectable({
	providedIn: 'root',
})
export class RoutingService {
	private readonly urlSegmentMap = new Map<WorkflowStep, string[]>();

	constructor(@Inject(APP_ROUTES) private readonly routes: SdmRoute[]) {
		this.registerRoutes(this.routes);
	}

	private get currentStepIndex(): number {
		const step = inject(ActivatedRoute).snapshot.data?.['workflowStep'];
		if (!step) return -1;

		return Array.from(this.urlSegmentMap.keys()).indexOf(step);
	}

	getLinkToNextStep(): string[] | null {
		return this.getUrlSegmentsAt(this.currentStepIndex + 1);
	}

	getLinkToPreviousStep(): string[] | null {
		return this.getUrlSegmentsAt(this.currentStepIndex - 1);
	}

	getLinkTo(step: WorkflowStep): string[] | null {
		return this.urlSegmentMap.get(step) ?? null;
	}

	private getUrlSegmentsAt(index: number): string[] | null {
		if (index < 0) return null;
		return Array.from(this.urlSegmentMap.values()).at(index) ?? null;
	}

	private registerRoutes(routes: SdmRoute[], urlSegments: string[] = ['/']) {
		routes.forEach((route) => {
			if (!route.path) return;
			const currentUrlSegments = [...urlSegments, route.path];

			const step = route.data?.workflowStep;
			if (step) {
				this.urlSegmentMap.set(step, currentUrlSegments);
				return;
			}

			const children = route.children;
			if (children) this.registerRoutes(children, currentUrlSegments);
		});
	}
}
