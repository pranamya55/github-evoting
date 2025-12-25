/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AfterContentInit,
	Component,
	inject,
	Input,
	Signal,
} from '@angular/core';
import { CandidateShortChoiceReturnCode } from '@vp/voter-portal-util-types';
import {
	Candidate,
	ElectionInformation,
	ElectionInformationAnswers,
	EmptyPosition,
} from 'e-voting-libraries-ui-kit';
import { getCandidateShortChoiceReturnCode } from '@vp/voter-portal-ui-state';
import { Store } from '@ngrx/store';

@Component({
	selector: 'vp-answer-candidate',
	templateUrl: './answer-candidate.component.html',
	standalone: false,
})
export class AnswerCandidateComponent implements AfterContentInit {
	@Input({ required: true }) headingLevel!: number;
	@Input({ required: true }) emptyPosition!: EmptyPosition;
	@Input({ required: true }) electionInformation!: ElectionInformation;
	@Input({ required: true }) electionInformationAnswers:
		| ElectionInformationAnswers
		| undefined;

	candidate?: Candidate;
	writeIn?: string;

	candidateShortChoiceReturnCode?: Signal<
		CandidateShortChoiceReturnCode | undefined
	>;

	private readonly store = inject(Store);

	get isAnswerUnknown(): boolean {
		return !this.electionInformationAnswers;
	}

	get isPositionEmpty(): boolean | undefined {
		if (this.isAnswerUnknown) return undefined;
		return !this.candidate && !this.writeIn;
	}

	get numberOfMandates(): number {
		return this.electionInformation.election.numberOfMandates;
	}

	get positionOnList(): number {
		return this.emptyPosition.positionOnList;
	}

	ngAfterContentInit() {
		const electionIdentification =
			this.electionInformation.election.electionIdentification;
		const positionOnList = this.emptyPosition.positionOnList;
		this.candidateShortChoiceReturnCode = this.store.selectSignal(
			getCandidateShortChoiceReturnCode(electionIdentification, positionOnList),
		);

		if (!this.electionInformationAnswers) return;

		const answerIndex = this.emptyPosition.positionOnList - 1;

		if (this.electionInformationAnswers.chosenWriteIns) {
			const chosenWriteIn =
				this.electionInformationAnswers.chosenWriteIns[answerIndex];
			if (chosenWriteIn?.writeIn) this.writeIn = chosenWriteIn.writeIn;
		}

		if (this.writeIn) return;

		const chosenCandidate =
			this.electionInformationAnswers.chosenCandidates[answerIndex];
		this.candidate =
			chosenCandidate &&
			this.electionInformation.candidates.find((candidate) => {
				return (
					candidate.candidateIdentification ===
					chosenCandidate.candidateIdentification
				);
			});
	}
}
