/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, inject, Input } from '@angular/core';
import { Candidate } from 'e-voting-libraries-ui-kit';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
	selector: 'vp-modal-candidate-selector-enabled',
	templateUrl: './modal-candidate-selector-enabled.component.html',
	standalone: false,
})
export class ModalCandidateSelectorEnabledComponent {
	@Input({ required: true }) candidate!: Candidate;
	@Input({ required: true }) isAlreadySelected!: boolean;

	private readonly modal = inject(NgbActiveModal);

	selectCandidate() {
		this.modal.close(this.candidate);
	}
}
