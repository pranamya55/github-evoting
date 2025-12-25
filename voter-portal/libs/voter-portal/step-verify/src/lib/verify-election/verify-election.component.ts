/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AfterContentInit, Component, inject, Input } from '@angular/core';
import {
	ElectionInformation,
	ElectionInformationAnswers,
} from 'e-voting-libraries-ui-kit';
import { Store } from '@ngrx/store';
import { getElectionInformationAnswer } from '@vp/voter-portal-ui-state';

@Component({
	selector: 'vp-verify-election',
	templateUrl: './verify-election.component.html',
	standalone: false,
})
export class VerifyElectionComponent implements AfterContentInit {
	@Input({ required: true }) electionGroupIdentification!: string;
	@Input({ required: true }) electionInformation!: ElectionInformation;

	electionInformationAnswers?: ElectionInformationAnswers;

	private readonly store = inject(Store);

	ngAfterContentInit(): void {
		this.electionInformationAnswers = this.store.selectSignal(
			getElectionInformationAnswer({
				electionGroupIdentification: this.electionGroupIdentification,
				electionIdentification:
					this.electionInformation.election.electionIdentification,
			}),
		)();
	}
}
