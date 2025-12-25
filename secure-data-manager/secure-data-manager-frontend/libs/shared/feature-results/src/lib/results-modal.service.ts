/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable, inject} from '@angular/core';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {ResultsModalComponent} from "./results-modal/results-modal.component";

@Injectable({
	providedIn: 'root',
})
export class ResultsModalService {
	private readonly modalService = inject(NgbModal);

	public showResults(ballotBoxId: string) {
		const modalOption = {fullscreen: 'xl', size: 'xl', scrollable: true, centered: false};
		const modalRef = this.modalService.open(ResultsModalComponent, modalOption);
		modalRef.componentInstance.ballotBoxId = ballotBoxId;
	}
}
