/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AfterContentInit,
	Component,
	DestroyRef,
	ElementRef,
	EventEmitter,
	inject,
	Input,
	OnChanges,
	Output,
	SimpleChanges,
	ViewChild,
} from '@angular/core';
import { FormGroupFrom } from '@vp/voter-portal-util-types';
import {
	Candidate,
	CandidatePosition,
	ChosenCandidate,
	ChosenWriteIn,
	ElectionInformation,
	EmptyPosition,
	TranslatableText,
	WriteInPosition,
} from 'e-voting-libraries-ui-kit';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChooseFormService } from '../choose-form.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ModalCandidateComponent } from '../modal-candidate/modal-candidate.component';
import { getAccumulation, isEligible } from '@vp/voter-portal-util-helpers';
import { FormControl } from '@angular/forms';

@Component({
	selector: 'vp-choose-election-candidate',
	templateUrl: './choose-election-candidate.component.html',
	providers: [ChooseFormService],
	standalone: false,
})
export class ChooseElectionCandidateComponent
	implements AfterContentInit, OnChanges
{
	@ViewChild('clearButton') clearButton?: ElementRef<HTMLButtonElement>;
	@Input({ required: true }) electionInformation!: ElectionInformation;
	@Input({ required: true }) emptyPosition!: EmptyPosition;
	@Input({ required: true }) nextAvailablePosition!: number;
	@Input({ required: true })
	candidatesChosenInCurrentElection!: ChosenCandidate[];
	@Input({ required: true }) candidatesChosenInPrimaryElection!:
		| Array<ChosenCandidate>
		| undefined;
	@Input({ required: true }) writeInsChosenInPrimaryElection!:
		| Array<ChosenWriteIn>
		| undefined;
	@Output() candidateAccumulated: EventEmitter<Candidate> = new EventEmitter();

	chosenCandidateFormGroup!: FormGroupFrom<ChosenCandidate>;
	chosenCandidate?: Candidate;
	candidateAccumulation = 0;
	isCandidateEligible = false;
	chosenWriteInFormGroup?: FormGroupFrom<ChosenWriteIn>;
	chosenWriteInPosition?: WriteInPosition;
	isFeedbackShown: { selection?: true; accumulation?: true; deletion?: true } =
		{};

	private readonly chooseFormService = inject(ChooseFormService);
	private readonly modalService = inject(NgbModal);
	private readonly destroyRef = inject(DestroyRef);

	get headingLevel(): number {
		return this.electionInformation.lists.length === 0 ? 3 : 4;
	}

	get positionOnList(): number {
		return this.emptyPosition.positionOnList;
	}

	get numberOfMandates(): number {
		return this.electionInformation.election.numberOfMandates;
	}

	get electionIdentification(): string {
		return this.electionInformation.election.electionIdentification;
	}

	get isPositionEmpty(): boolean {
		return !this.chosenCandidate && !this.chosenWriteInPosition;
	}

	get candidateDescription(): TranslatableText | null {
		const chosenCandidateDescription =
			this.chosenCandidate?.displayCandidateLine1;
		return chosenCandidateDescription || null;
	}

	get isMaximumAccumulationReached(): boolean {
		return (
			this.candidateAccumulation ===
			this.electionInformation.election.candidateAccumulation
		);
	}

	get isAccumulationPossible(): boolean {
		return (
			this.isCandidateEligible &&
			this.nextAvailablePosition >= 0 &&
			!this.isMaximumAccumulationReached
		);
	}

	get writeInControl(): FormControl {
		return this.chosenWriteInFormGroup?.controls.writeIn as FormControl;
	}

	ngOnChanges(changes: SimpleChanges) {
		if (changes['candidatesChosenInCurrentElection'])
			this.updateCandidateAccumulation();
		if (changes['candidatesChosenInPrimaryElection'])
			this.preventIneligibleCandidate();
	}

	ngAfterContentInit() {
		this.chosenCandidateFormGroup =
			this.chooseFormService.createChosenCandidateFormGroup(
				this.electionInformation,
			);
		this.updateChosenCandidateWhenFromValueChanges();

		if (this.electionInformation.election.writeInsAllowed) {
			this.chosenWriteInFormGroup =
				this.chooseFormService.createChosenWriteInFormGroup(
					this.electionInformation,
					this.positionOnList,
				);
			this.updateChosenWriteInWhenFromValueChanges();
		}

		this.updateCandidateAccumulation();
		this.preventIneligibleCandidate();
	}

	openCandidateSelectionModal() {
		const modalOptions = { fullscreen: 'xl', size: 'xl' };
		const modalRef = this.modalService.open(
			ModalCandidateComponent,
			modalOptions,
		);

		Object.assign(modalRef.componentInstance, {
			electionInformation: this.electionInformation,
			chosenCandidate: this.chosenCandidate,
			candidatesChosenInCurrentElection: this.candidatesChosenInCurrentElection,
			candidatesChosenInPrimaryElection: this.candidatesChosenInPrimaryElection,
		});

		modalRef.result
			.then((chosenCandidate: Candidate) => {
				if (chosenCandidate) {
					this.selectCandidate(chosenCandidate);
				} else {
					this.selectWriteIn();
				}

				this.isFeedbackShown = { selection: true };
			})
			.catch(() => {
				/* do nothing when modal is dismissed */
			});
	}

	selectCandidate(candidate: Candidate, candidatePosition?: CandidatePosition) {
		// update candidate answer
		this.chosenCandidateFormGroup.controls.candidateIdentification.setValue(
			candidate.candidateIdentification,
		);

		const { candidateListIdentification } =
			this.chosenCandidateFormGroup.controls;
		if (candidatePosition && candidateListIdentification)
			candidateListIdentification.setValue(
				candidatePosition.candidateListIdentification,
			);

		// clear chosen write-in
		this.chosenWriteInFormGroup?.reset();
	}

	selectWriteIn() {
		if (!this.chosenWriteInFormGroup) return;

		// update write-in answer
		this.chosenWriteInFormGroup.controls.writeIn.setValue('');

		// clear chosen candidate
		this.chosenCandidateFormGroup.reset();
	}

	accumulateCandidate() {
		this.candidateAccumulated.emit(this.chosenCandidate);
		this.isFeedbackShown = { accumulation: true };

		setTimeout(() => {
			if (!this.isMaximumAccumulationReached || !this.clearButton) return;
			this.clearButton.nativeElement.focus();
		});
	}

	unselectCandidate() {
		this.chosenCandidateFormGroup.reset();
		this.chosenWriteInFormGroup?.reset();

		this.isFeedbackShown = { deletion: true };

		setTimeout(() => {
			const selectionButton = document.querySelector<HTMLButtonElement>(
				`#candidate-${this.positionOnList}-election-${this.electionIdentification}-selection`,
			);
			if (selectionButton) selectionButton.focus();
		});
	}

	removeScreenReaderFeedbacks() {
		this.isFeedbackShown = {};
	}

	/**
	 * Observes changes to the chosen candidate and find the matching candidate in the list of candidates.
	 */
	private updateChosenCandidateWhenFromValueChanges() {
		this.chosenCandidateFormGroup.controls.candidateIdentification.valueChanges
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe((candidateIdentification) => {
				if (candidateIdentification === null) {
					this.chosenCandidate = undefined;
					return;
				}

				this.chosenCandidate = this.electionInformation.candidates.find(
					(candidate) => {
						return (
							candidate.candidateIdentification === candidateIdentification
						);
					},
				);

				this.preventIneligibleCandidate();
			});
	}

	/**
	 * Observes changes to the chosen write-in candidate.
	 */
	private updateChosenWriteInWhenFromValueChanges() {
		if (!this.chosenWriteInFormGroup) return;

		this.chosenWriteInFormGroup.controls.writeIn.valueChanges
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe((writeIn) => {
				if (writeIn === null) {
					this.chosenWriteInPosition = undefined;
					return;
				}

				this.chosenWriteInPosition =
					this.electionInformation.writeInPositions[this.positionOnList - 1];
			});
	}

	/**
	 * Reset the chosenCandidateAnswer if the chosenCandidate is not eligible.
	 */
	private preventIneligibleCandidate() {
		if (!this.chosenCandidate) {
			this.isCandidateEligible = false;
			return;
		}

		this.isCandidateEligible = isEligible(
			this.chosenCandidate,
			this.candidatesChosenInPrimaryElection,
		);

		if (!this.isCandidateEligible) this.chosenCandidateFormGroup.reset();
	}

	/**
	 * Updates the candidateAccumulation with the number of positions where the current candidate is selected.
	 */
	private updateCandidateAccumulation() {
		if (!this.chosenCandidate) {
			this.candidateAccumulation = 0;
			return;
		}

		this.candidateAccumulation = getAccumulation(
			this.chosenCandidate,
			this.candidatesChosenInCurrentElection,
		);
	}
}
