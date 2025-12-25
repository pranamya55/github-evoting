/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, inject, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ProcessCancellationService } from "@vp/voter-portal-ui-services";
import { CancelMode } from "@vp/voter-portal-util-types";

@Component({
	selector: 'vp-modal-invalid-codes',
	standalone: false,
	templateUrl: './modal-invalid-codes.component.html',
})
export class ModalInvalidCodesComponent {
	@Input({ required: true }) title!: string;
	@Input({ required: true }) message!: string;
	@Input({ required: true }) cancelMode!: CancelMode;

	private readonly activeModal = inject(NgbActiveModal);
	private readonly processCancellation = inject(ProcessCancellationService);

	checkAgain() {
		this.activeModal.close();
		setTimeout(() => {
			document.documentElement.scrollTo({top: 0, behavior: 'smooth'});
		});
	}

	quit() {
		this.activeModal.close();
		this.processCancellation.confirm(this.cancelMode);
	}
}
