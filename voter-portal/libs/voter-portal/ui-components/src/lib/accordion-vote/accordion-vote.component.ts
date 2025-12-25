/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, ContentChild, Input, TemplateRef } from '@angular/core';
import {
	Ballot,
	isVariantBallot,
	StandardBallot,
	StandardQuestion,
	TieBreakQuestion,
	VoteTexts,
} from 'e-voting-libraries-ui-kit';

@Component({
	selector: 'vp-accordion-vote',
	templateUrl: './accordion-vote.component.html',
	standalone: false,
})
export class AccordionVoteComponent {
	@Input({ required: true }) voteTexts!: VoteTexts;
	@Input() headingLevel = 3;
	@ContentChild(TemplateRef) questionTemplate?: TemplateRef<{
		ballotIdentification: Ballot['ballotIdentification'];
		question: StandardBallot | StandardQuestion | TieBreakQuestion;
	}>;

	protected readonly isVariantBallot = isVariantBallot;
}
