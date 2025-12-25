/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, ContentChild, Input, TemplateRef} from '@angular/core';
import {ElectionInformation, EmptyPosition} from 'e-voting-libraries-ui-kit';

@Component({
	selector: 'vp-accordion-election',
	standalone: false,
	templateUrl: './accordion-election.component.html',
})
export class AccordionElectionComponent {
	@ContentChild('listTemplate') listTemplate?: TemplateRef<{}>;
	@ContentChild('candidateTemplate') candidateTemplate?: TemplateRef<{
		emptyPosition: EmptyPosition;
	}>;
	@Input({ required: true }) electionInformation!: ElectionInformation;
	@Input() chosenCandidateCount?: number;
	@Input() headingLevel = 3;

	get numberOfMandates(): number {
		return this.electionInformation.election.numberOfMandates;
	}
}
