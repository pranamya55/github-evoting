/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, DestroyRef, inject, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Candidate, List, TranslateTextPipe } from 'e-voting-libraries-ui-kit';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
	selector: 'vp-modal-list',
	templateUrl: './modal-list.component.html',
	providers: [TranslateTextPipe],
	standalone: false,
})
export class ModalListComponent implements OnInit {
	@Input({ required: true }) lists!: List[];
	@Input({ required: true }) candidates!: Candidate[];
	@Input({ required: true }) chosenList!: List | undefined;
	readonly activeModal = inject(NgbActiveModal);
	readonly filter = new FormGroup({
		displayListLine1: new FormControl<string>('', { nonNullable: true }),
	});

	filteredLists!: List[];

	private readonly destroyRef: DestroyRef = inject(DestroyRef);
	private readonly translateTextPipe: TranslateTextPipe =
		inject(TranslateTextPipe);

	get displayListLine1() {
		return this.filter.controls.displayListLine1;
	}

	get searchResultsProperties(): object {
		return {
			resultCount: this.filteredLists.length,
			listCount: this.lists.length,
			searchTerm: this.displayListLine1.value,
		};
	}

	ngOnInit() {
		this.filterLists();
		this.filter.valueChanges
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe(() => {
				this.filterLists();
			});
	}

	private filterLists() {
		const { displayListLine1 } = this.filter.getRawValue();

		this.filteredLists = this.lists.filter(
			(list) =>
				!displayListLine1 ||
				this.translateTextPipe
					.transform(list.displayListLine1)
					.toLowerCase()
					.includes(displayListLine1.toLowerCase()),
		);
	}
}
