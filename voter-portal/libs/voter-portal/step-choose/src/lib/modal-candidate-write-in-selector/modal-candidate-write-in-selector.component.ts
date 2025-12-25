/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, inject } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
	selector: 'vp-modal-candidate-write-in-selector',
	templateUrl: './modal-candidate-write-in-selector.component.html',
	standalone: false,
})
export class ModalCandidateWriteInSelectorComponent {
	private readonly modal = inject(NgbActiveModal);

	selectWriteIn() {
		this.modal.close();
	}
}
