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
import { Store } from '@ngrx/store';
import { getListShortChoiceReturnCode } from '@vp/voter-portal-ui-state';
import { ShortChoiceReturnCode } from '@vp/voter-portal-util-types';
import {
	ElectionInformation,
	ElectionInformationAnswers,
	List,
} from 'e-voting-libraries-ui-kit';

@Component({
	selector: 'vp-answer-list',
	templateUrl: './answer-list.component.html',
	standalone: false,
})
export class AnswerListComponent implements AfterContentInit {
	@Input({ required: true }) electionInformation!: ElectionInformation;
	@Input({ required: true }) electionInformationAnswers:
		| ElectionInformationAnswers
		| undefined;

	list?: List;
	listShortChoiceReturnCode?: Signal<ShortChoiceReturnCode | undefined>;

	private readonly store = inject(Store);

	get isAnswerUnknown(): boolean {
		return !this.electionInformationAnswers;
	}

	ngAfterContentInit() {
		const electionIdentification =
			this.electionInformation.election.electionIdentification;
		this.listShortChoiceReturnCode = this.store.selectSignal(
			getListShortChoiceReturnCode(electionIdentification),
		);

		if (
			this.electionInformationAnswers &&
			'chosenList' in this.electionInformationAnswers
		) {
			const chosenListIdentification =
				this.electionInformationAnswers.chosenList?.listIdentification;
			this.list = this.electionInformation.lists.find(
				(list: List) => list.listIdentification === chosenListIdentification,
			);
		}
	}
}
