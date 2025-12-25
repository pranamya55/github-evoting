/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, inject, Input, OnInit } from '@angular/core';
import {
	Candidate,
	CandidatePosition,
	List,
	TranslatableText,
} from 'e-voting-libraries-ui-kit';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

interface ListCandidate {
	positionOnList: number;
	displayCandidateLine1: TranslatableText;
}

@Component({
	selector: 'vp-modal-list-selector',
	templateUrl: './modal-list-selector.component.html',
	standalone: false,
})
export class ModalListSelectorComponent implements OnInit {
	@Input({ required: true }) list!: List;
	@Input({ required: true }) candidates!: Candidate[];
	@Input({ required: true }) isSelected!: boolean;

	listCandidates: ListCandidate[] = [];
	areDetailsCollapsed = true;

	private readonly modal: NgbActiveModal = inject(NgbActiveModal);

	ngOnInit(): void {
		this.listCandidates = this.list.candidatePositions
			.map(
				(candidatePosition: CandidatePosition) =>
					({
						positionOnList: candidatePosition.positionOnList,
						displayCandidateLine1: this.candidates.find(
							(candidate) =>
								candidate.candidateIdentification ===
								candidatePosition.candidateIdentification,
						)?.displayCandidateLine1,
					}) as ListCandidate,
			)
			.filter(
				(listCandidate: ListCandidate) =>
					listCandidate.displayCandidateLine1 !== undefined,
			);
	}

	selectList(): void {
		this.modal.close(this.list);
	}
}
