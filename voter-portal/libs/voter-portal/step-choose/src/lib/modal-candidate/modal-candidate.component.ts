/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	AfterContentInit,
	Component,
	DestroyRef,
	inject,
	Input,
	OnChanges,
	SimpleChanges,
} from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { FormControl, FormGroup } from '@angular/forms';
import {
	Candidate,
	ChosenCandidate,
	ElectionInformation,
	Eligibility,
	List,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
	selector: 'vp-modal-candidate',
	templateUrl: './modal-candidate.component.html',
	providers: [TranslateTextPipe],
	standalone: false,
})
export class ModalCandidateComponent implements AfterContentInit, OnChanges {
	@Input({ required: true }) electionInformation!: ElectionInformation;
	@Input({ required: true }) chosenCandidate!: Candidate | undefined;
	@Input({ required: true })
	candidatesChosenInCurrentElection!: ChosenCandidate[];
	@Input({ required: true }) candidatesChosenInPrimaryElection!:
		| Array<ChosenCandidate>
		| undefined;

	readonly activeModal = inject(NgbActiveModal);
	readonly filters = new FormGroup({
		displayCandidateLine1: new FormControl<string>('', { nonNullable: true }),
		onlyCandidatesChosenInCurrentElection: new FormControl<boolean>(false, {
			nonNullable: true,
		}),
		listIdentification: new FormControl<string | null>(null),
	});

	filteredCandidates: Candidate[] = [];

	private readonly destroyRef = inject(DestroyRef);
	private readonly translateTextPipe = inject(TranslateTextPipe);
	private sortedCandidates: Candidate[] = [];
	private chosenCandidateIds!: ReadonlySet<
		Candidate['candidateIdentification']
	>;
	private candidateIdsByList!: ReadonlyMap<
		List['listIdentification'],
		ReadonlySet<Candidate['candidateIdentification']>
	>;

	get displayCandidateLine1() {
		return this.filters.controls.displayCandidateLine1;
	}

	get searchResultsProperties(): object {
		return {
			resultCount: this.filteredCandidates.length,
			candidateCount: this.electionInformation.candidates.length,
			searchTerm: this.displayCandidateLine1.value,
		};
	}

	ngOnChanges(changes: SimpleChanges) {
		if (changes['candidatesChosenInCurrentElection'])
			this.updateChosenCandidateIds();
	}

	ngAfterContentInit() {
		// move implicitly eligible candidates to the top of the list
		this.sortedCandidates = [
			...this.electionInformation.candidates.filter(
				(candidate) => candidate.eligibility !== Eligibility.EXPLICIT,
			),
			...this.electionInformation.candidates.filter(
				(candidate) => candidate.eligibility === Eligibility.EXPLICIT,
			),
		];

		this.updateChosenCandidateIds();

		this.filterCandidates();
		this.filters.valueChanges
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe(() => {
				this.filterCandidates();
			});

		if (this.electionInformation.lists.length === 0) return;

		this.candidateIdsByList = new Map(
			this.electionInformation.lists.map((list) => {
				const candidateIds = list.candidatePositions.map(
					(candidatePosition) => candidatePosition.candidateIdentification,
				);
				return [list.listIdentification, new Set(candidateIds)];
			}),
		);
	}

	private filterCandidates() {
		const {
			displayCandidateLine1,
			onlyCandidatesChosenInCurrentElection,
			listIdentification,
		} = this.filters.getRawValue();

		let candidates = this.sortedCandidates;

		const candidateIdsInSelectedList =
			listIdentification && this.candidateIdsByList.get(listIdentification);
		if (candidateIdsInSelectedList) {
			candidates = candidates.filter((candidate) =>
				candidateIdsInSelectedList.has(candidate.candidateIdentification),
			);
		}

		this.filteredCandidates = candidates.filter((candidate) => {
			const candidateText = this.translateTextPipe.transform(
				candidate.displayCandidateLine1,
			);
			const matchesDisplayCandidateFilter =
				!displayCandidateLine1 ||
				candidateText
					.toLowerCase()
					.includes(displayCandidateLine1.toLowerCase());
			const matchesOnlyChosenCandidatesFilter =
				!onlyCandidatesChosenInCurrentElection ||
				this.chosenCandidateIds.has(candidate.candidateIdentification);

			return matchesDisplayCandidateFilter && matchesOnlyChosenCandidatesFilter;
		});
	}

	private updateChosenCandidateIds() {
		this.chosenCandidateIds = new Set(
			this.candidatesChosenInCurrentElection
				.map((candidate) => candidate.candidateIdentification)
				.filter(
					(candidateIdentification): candidateIdentification is string =>
						candidateIdentification !== null,
				),
		);
	}
}
