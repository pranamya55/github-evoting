/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Injectable, inject } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmationModalConfig } from '@vp/voter-portal-util-types';
import { from, Observable, of } from 'rxjs';
import { catchError, filter } from 'rxjs/operators';
import { ConfirmationModalComponent } from './confirmation-modal/confirmation-modal.component';

@Injectable({
	providedIn: 'root',
})
export class ConfirmationService {
	private readonly modalService = inject(NgbModal);

	confirm(config: ConfirmationModalConfig): Observable<boolean> {
		const modalRef = this.modalService.open(
			ConfirmationModalComponent,
			config.modalOptions,
		);

		modalRef.componentInstance.content = config.content;
		modalRef.componentInstance.title = config.title ?? 'common.confirmaction';
		modalRef.componentInstance.confirmIcon = config.confirmIcon;
		modalRef.componentInstance.confirmLabel =
			config.confirmLabel ?? 'common.confirm';
		modalRef.componentInstance.cancelLabel =
			config.cancelLabel ?? 'common.cancel';

		return from(modalRef.result).pipe(
			catchError(() => of(null)),
			filter(
				(modalResult: boolean | null): modalResult is boolean =>
					modalResult !== null,
			),
		);
	}
}
