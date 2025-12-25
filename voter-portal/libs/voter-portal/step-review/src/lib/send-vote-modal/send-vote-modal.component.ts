/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, inject, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Store } from '@ngrx/store';
import {
	getDefinedBackendError,
	getDefinedShortChoiceReturnCodes,
} from '@vp/voter-portal-ui-state';
import { merge } from 'rxjs';
import { take } from 'rxjs/operators';

@Component({
	selector: 'vp-progress',
	templateUrl: './send-vote-modal.component.html',
	standalone: false,
})
export class SendVoteModalComponent implements OnInit {
	private readonly store = inject(Store);
	private readonly activeModal = inject(NgbActiveModal);

	ngOnInit() {
		const shortChoiceReturnCodes$ = this.store.pipe(
			getDefinedShortChoiceReturnCodes,
		);
		const backendError$ = this.store.pipe(getDefinedBackendError);

		merge(shortChoiceReturnCodes$, backendError$)
			.pipe(take(1))
			.subscribe(() => {
				this.activeModal.close();
			});
	}
}
