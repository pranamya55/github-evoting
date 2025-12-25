/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {FormsModule} from '@angular/forms';
import {Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild,} from '@angular/core';

import {BallotBox} from '@sdm/shared-util-types';
import {TranslateModule} from '@ngx-translate/core';
import {BallotBoxListItemComponent} from '../ballot-box-list-item/ballot-box-list-item.component';

@Component({
	selector: 'sdm-ballot-box-list',
	standalone: true,
	imports: [
		FormsModule,
		TranslateModule,
		BallotBoxListItemComponent
	],
	templateUrl: './ballot-box-list.component.html',
})
export class BallotBoxListComponent implements OnChanges {
	@ViewChild('mainCheckbox') mainCheckbox?: ElementRef<HTMLInputElement>;
	@Input({required: true}) ballotBoxes!: Set<BallotBox>;
	@Input({required: true}) describedById!: string;
	@Input() showIdle = true;
	@Input() enableCheckbox = true;
	@Output() ballotBoxesSelected: EventEmitter<BallotBox[]> = new EventEmitter();
	selectableBallotBoxes: Set<BallotBox> = new Set();
	selectedBallotBoxes: Set<BallotBox> = new Set();

	get allSelected(): boolean {
		return this.selectedBallotBoxes.size === this.selectableBallotBoxes.size;
	}

	get someSelected(): boolean {
		return this.selectedBallotBoxes.size !== 0;
	}

	ngOnChanges(changes: SimpleChanges) {
		if (!changes['ballotBoxes']) return;

		const ballotBoxes = Array.from(this.ballotBoxes);

		// clear all ballot boxes and add them back in the correct order
		this.ballotBoxes.clear();

		this.sortBallotBoxes(ballotBoxes).forEach((ballotBox) => {
			this.ballotBoxes.add(ballotBox);
		});
	}

	toggleFromSelectableList(ballotBox: BallotBox, isSelectable: boolean) {
		setTimeout(() => {
			if (isSelectable) {
				this.selectableBallotBoxes.add(ballotBox);
			} else {
				this.selectableBallotBoxes.delete(ballotBox);
				this.selectOrUnselect(ballotBox, false);
			}
		});
	}

	selectOrUnselectAll(shouldSelectAll: boolean) {
		if (shouldSelectAll) {
			this.selectedBallotBoxes = new Set(this.selectableBallotBoxes);
		} else {
			this.selectedBallotBoxes.clear();
		}
		this.ballotBoxesSelected.emit(
			this.sortBallotBoxes(Array.from(this.selectedBallotBoxes.values()))
		);
	}

	selectOrUnselect(ballotBox: BallotBox, shouldSelect: boolean) {
		if (shouldSelect) {
			this.selectedBallotBoxes.add(ballotBox);
		} else {
			this.selectedBallotBoxes.delete(ballotBox);
		}

		this.ballotBoxesSelected.emit(
			this.sortBallotBoxes(Array.from(this.selectedBallotBoxes.values()))
		);

		if (!this.mainCheckbox) return;

		this.mainCheckbox.nativeElement.indeterminate = this.someSelected && !this.allSelected;
	}

	sortBallotBoxes(ballotBoxes: BallotBox[]): BallotBox[] {
		return [...ballotBoxes].sort((a, b) => {
			// first sort by category: test ballot boxes before regular ballot boxes
			if (a.test !== b.test) return a.test ? -1 : 1;

			// then sort by title: alphabetical order
			return a.description.localeCompare(b.description);
		});
	}

	protected readonly resizeBy = resizeBy;
}
