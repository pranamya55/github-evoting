/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Store} from "@ngrx/store";
import {FAQSection} from "@vp/voter-portal-util-types";
import {Component, inject} from '@angular/core';
import {getVoteCastInPreviousSession} from "@vp/voter-portal-ui-state";

@Component({
	selector: 'vp-finalization-page',
	templateUrl: './finalization-page.component.html',
	standalone: false
})
export class FinalizationPageComponent {
	private readonly store = inject(Store);
	protected readonly FAQSection = FAQSection;

	voteCastInPreviousSession$ = this.store.select(
		getVoteCastInPreviousSession,
	);
}
