/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AfterContentInit, Component, Input } from '@angular/core';
import {
	Candidate,
	ChosenCandidate,
	ElectionInformation,
} from 'e-voting-libraries-ui-kit';
import { getAccumulation, isEligible } from '@vp/voter-portal-util-helpers';

@Component({
	selector: 'vp-modal-candidate-selector',
	templateUrl: './modal-candidate-selector.component.html',
	standalone: false,
})
export class ModalCandidateSelectorComponent implements AfterContentInit {
	@Input({ required: true }) electionInformation!: ElectionInformation;
	@Input({ required: true }) candidate!: Candidate;
	@Input({ required: true }) isSelectedOnCurrentPosition!: boolean;
	@Input({ required: true })
	candidatesChosenInCurrentElection!: Array<ChosenCandidate>;
	@Input({ required: true }) candidatesChosenInPrimaryElection!:
		| Array<ChosenCandidate>
		| undefined;

	isEligible!: boolean;
	candidateAccumulation!: number;
	hasReachedMaximumAccumulation!: boolean;

	ngAfterContentInit() {
		this.isEligible = isEligible(
			this.candidate,
			this.candidatesChosenInPrimaryElection,
		);
		this.candidateAccumulation = getAccumulation(
			this.candidate,
			this.candidatesChosenInCurrentElection,
		);
		this.hasReachedMaximumAccumulation =
			this.candidateAccumulation ===
			this.electionInformation.election.candidateAccumulation;
	}
}
