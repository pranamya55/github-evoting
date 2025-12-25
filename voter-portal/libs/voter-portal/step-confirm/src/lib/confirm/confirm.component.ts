/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Store} from '@ngrx/store';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ConfirmActions} from '@vp/voter-portal-ui-state';
import {Component, inject} from '@angular/core';
import {CancelMode, FAQSection} from '@vp/voter-portal-util-types';
import {ModalInvalidCodesComponent} from '@vp/voter-portal-ui-components';

@Component({
	selector: 'vp-confirm',
	templateUrl: './confirm.component.html',
	standalone: false,
})
export class ConfirmComponent {
	private readonly store = inject(Store);
	private readonly modalService = inject(NgbModal);

	readonly FAQSection = FAQSection;

	openInvalidCodesModal() {
		const modalRef = this.modalService.open(ModalInvalidCodesComponent, {
			size: 'lg',
			backdrop: "static",
			keyboard: false,
		});
		Object.assign(modalRef.componentInstance, {
			title: 'confirm.codesdonotmatch.title',
			message: 'confirm.codesdonotmatch.message',
			cancelMode: CancelMode.QuitProcess,
		});
	}

	quit() {
		this.store.dispatch(ConfirmActions.endClicked());
	}
}
