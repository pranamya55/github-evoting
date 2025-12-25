/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, Input } from '@angular/core';

let nextIndex = 0;

@Component({
	selector: 'vp-modal-candidate-selector-disabled',
	templateUrl: './modal-candidate-selector-disabled.component.html',
	standalone: false,
})
export class ModalCandidateSelectorDisabledComponent {
	@Input({ required: true }) isEligible!: boolean;
	@Input({ required: true }) isSelectedOnCurrentPosition!: boolean;
	@Input({ required: true }) hasReachedMaximumAccumulation!: boolean;
	@Input({ required: true }) maximumAccumulation!: number;

	selectorIndex = nextIndex++;
}
