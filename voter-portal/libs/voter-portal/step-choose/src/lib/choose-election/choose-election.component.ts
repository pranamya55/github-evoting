/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AfterContentInit,
	AfterViewInit,
	Component,
	DestroyRef,
	inject,
	Input,
	Signal,
} from '@angular/core';
import { ControlContainer } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FAQSection, FormArrayFrom, FormGroupFrom } from '@vp/voter-portal-util-types';
import {
	Candidate,
	CandidatePosition,
	ChosenCandidate,
	ChosenWriteIn,
	ElectionInformation,
	ElectionInformationAnswers,
	ElectionRelation,
	List,
	ReferencedElectionInformation,
} from 'e-voting-libraries-ui-kit';
import { FAQService } from '@vp/voter-portal-feature-faq';
import { ChooseFormService } from '../choose-form.service';
import { debounceTime, distinctUntilChanged, Observable } from 'rxjs';
import { getRawValueObservable } from '@vp/voter-portal-util-helpers';
import { Store } from '@ngrx/store';
import { getHasSubmittedAnswer } from '@vp/voter-portal-ui-state';

@Component({
	selector: 'vp-choose-election',
	templateUrl: './choose-election.component.html',
	providers: [ChooseFormService],
	standalone: false,
})
export class ChooseElectionComponent
	implements AfterContentInit, AfterViewInit
{
	@Input({ required: true }) electionInformation!: ElectionInformation;

	electionInformationAnswerFormGroup!: FormGroupFrom<ElectionInformationAnswers>;
	candidatesChosenInCurrentElection: ChosenCandidate[] = [];
	candidatesChosenInPrimaryElection?: ChosenCandidate[];
	writeInsChosenInPrimaryElection?: ChosenWriteIn[];
	referencedPrimaryElection: string | undefined;
	nextAvailablePosition = 0;

	private readonly controlContainer = inject(ControlContainer);
	private readonly chooseFormService = inject(ChooseFormService);
	private readonly destroyRef = inject(DestroyRef);
	private readonly faqService = inject(FAQService);
	private readonly store = inject(Store);

	readonly shouldShowErrors: Signal<boolean> = this.store.selectSignal(
		getHasSubmittedAnswer,
	);

	get electionsInformationFormArray() {
		return this.controlContainer
			.control as FormArrayFrom<ElectionInformationAnswers>;
	}

	get chosenCandidateCount(): number {
		return this.candidatesChosenInCurrentElection.length;
	}

	get numberOfMandates(): number {
		return this.electionInformation.election.numberOfMandates;
	}

	ngAfterContentInit(): void {
		this.electionInformationAnswerFormGroup =
			this.chooseFormService.createElectionInformationAnswersFormGroup(
				this.electionInformation,
			);

		this.referencedPrimaryElection =
			this.getReferencedPrimaryElectionInformation()?.referencedElection;
		if (this.referencedPrimaryElection) {
			this.candidatesChosenInPrimaryElection = [];
			if (this.electionInformation.election.writeInsAllowed) {
				this.writeInsChosenInPrimaryElection = [];
			}
		}
	}

	ngAfterViewInit(): void {
		// update candidatesChosenInCurrentElection and nextAvailablePosition with all changes in the chosenCandidates of the current election
		this.observeChosenCandidatesChanges(this.electionInformationAnswerFormGroup)
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe((chosenCandidates) => {
				this.candidatesChosenInCurrentElection =
					this.getChosenCandidatesWithIdentification(chosenCandidates);
				this.nextAvailablePosition =
					this.getFirstChosenCandidatesWithoutIdentification(chosenCandidates);

				// unselect the list if to many candidates where removed
				if (
					this.chosenCandidateCount <
					this.electionInformation.election.minimalCandidateSelectionInList
				) {
					this.electionInformationAnswerFormGroup.controls.chosenList?.reset();
				}
			});

		// get potential primary election
		const primaryElectionInformationAnswers =
			this.getPrimaryElectionInformationAnswers();

		if (primaryElectionInformationAnswers) {
			// if there is a primary election, update candidatesChosenInPrimaryElection with all changes in the chosenCandidates of the primary election
			this.observeChosenCandidatesChanges(primaryElectionInformationAnswers)
				.pipe(takeUntilDestroyed(this.destroyRef))
				.subscribe((chosenCandidates) => {
					this.candidatesChosenInPrimaryElection =
						this.getChosenCandidatesWithIdentification(chosenCandidates);
				});
			// and if the election has write-ins, update writeInsChosenInPrimaryElection with all changes in the chosenWriteIns of the primary election
			if (this.electionInformation.election.writeInsAllowed) {
				this.observeChosenWriteInsChanges(primaryElectionInformationAnswers)
					.pipe(takeUntilDestroyed(this.destroyRef))
					.subscribe(
						(chosenWriteIns) =>
							(this.writeInsChosenInPrimaryElection = chosenWriteIns?.filter(
								(chosenWriteIn) => chosenWriteIn.writeIn !== null,
							)),
					);
			}
		}
	}

	/**
	 * Opens the FAQ modal with the "How to Use Write-ins" section expanded.
	 */
	showWriteInsFAQ() {
		this.faqService.showFAQ(FAQSection.HowToUseWriteIns);
	}

	/**
	 * Fills the first available position of the chosenCandidates with the given candidate.
	 */
	accumulateCandidate(candidate: Candidate): void {
		const chosenCandidatesFormArray =
			this.electionInformationAnswerFormGroup.controls.chosenCandidates;
		const accumulationFormGroup =
			this.nextAvailablePosition >= 0 &&
			chosenCandidatesFormArray.controls[this.nextAvailablePosition];

		if (!accumulationFormGroup) return;

		accumulationFormGroup.patchValue({
			candidateIdentification: candidate.candidateIdentification,
		});
	}

	/**
	 * Fills all the positions of the chosenCandidates with the corresponding candidate positions of the given list, and empties the remaining positions.
	 * If the empty list is chosen, all positions are emptied.
	 */
	chooseListCandidatePositions(list: List | undefined): void {
		const candidatePositionMap: ReadonlyMap<number, CandidatePosition> =
			new Map(
				list?.candidatePositions.map((candidatePosition) => [
					candidatePosition.positionOnList,
					candidatePosition,
				]),
			);

		const newChosenCandidates: ChosenCandidate[] =
			this.electionInformation.emptyList.emptyPositions.map((emptyPosition) => {
				const candidatePosition = candidatePositionMap.get(
					emptyPosition.positionOnList,
				);
				return (candidatePosition as ChosenCandidate) ?? {};
			});

		this.electionInformationAnswerFormGroup.controls.chosenCandidates.reset(
			newChosenCandidates,
		);
	}

	/**
	 * Returns the answers of the primary election associated to the current election if there is one.
	 */
	private getPrimaryElectionInformationAnswers():
		| FormGroupFrom<ElectionInformationAnswers>
		| undefined {
		if (!this.referencedPrimaryElection) return;

		return this.electionsInformationFormArray.controls.find(
			(control) =>
				control.value.electionIdentification === this.referencedPrimaryElection,
		);
	}

	/**
	 * Returns the information of the primary election associated to the current election if there is one.
	 */
	private getReferencedPrimaryElectionInformation():
		| ReferencedElectionInformation
		| undefined {
		return this.electionInformation.election.referencedElectionsInformation?.find(
			(referencedElectionInformation) => {
				return (
					referencedElectionInformation.electionRelation ===
					ElectionRelation.PRIMARY
				);
			},
		);
	}

	/**
	 * Returns an array of chosenCandidates whose candidateIdentification are not null.
	 */
	private getChosenCandidatesWithIdentification(
		chosenCandidates: ChosenCandidate[],
	): ChosenCandidate[] {
		return chosenCandidates.filter(
			(candidate) => candidate.candidateIdentification !== null,
		);
	}

	/**
	 * Returns the first chosenCandidates whose candidateIdentification is null.
	 */
	private getFirstChosenCandidatesWithoutIdentification(
		chosenCandidates: ChosenCandidate[],
	): number {
		return chosenCandidates.findIndex(
			(candidate) => candidate.candidateIdentification === null,
		);
	}

	/**
	 * Returns an observable emitting up-to-date chosenCandidates whenever their value changes.
	 */
	private observeChosenCandidatesChanges(
		electionInformationAnswerFormGroup: FormGroupFrom<ElectionInformationAnswers>,
	): Observable<ChosenCandidate[]> {
		return getRawValueObservable(
			electionInformationAnswerFormGroup,
			'chosenCandidates',
		);
	}

	/**
	 * Returns an observable emitting up-to-date ChosenWriteIns whenever their value changes.
	 */
	private observeChosenWriteInsChanges(
		electionInformationAnswerFormGroup: FormGroupFrom<ElectionInformationAnswers>,
	): Observable<ChosenWriteIn[] | undefined> {
		return getRawValueObservable(
			electionInformationAnswerFormGroup,
			'chosenWriteIns',
		).pipe(debounceTime(200), distinctUntilChanged());
	}
}
