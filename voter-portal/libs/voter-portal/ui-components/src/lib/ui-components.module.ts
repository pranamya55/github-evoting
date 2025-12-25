/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {RouterLink} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {UiDirectivesModule} from '@vp/voter-portal-ui-directives';
import {BackendErrorComponent} from './backend-error/backend-error.component';
import {ClearableInputComponent} from './clearable-input/clearable-input.component';
import {FooterComponentComponent} from './footer/footer.component';
import {AnswerComponent} from './answer/answer.component';
import {AccordionVoteComponent} from './accordion-vote/accordion-vote.component';
import {AccordionElectionComponent} from './accordion-election/accordion-election.component';
import {AnswerCandidateComponent} from './answer-candidate/answer-candidate.component';
import {AnswerListComponent} from './answer-list/answer-list.component';
import {ModalInvalidCodesComponent} from './modal-invalid-codes/modal-invalid-codes.component';
import {FinalizationCodeComponent} from "./finalization-code/finalization-code.component";
import {
	ConcatPipe,
	MarkdownPipe,
	SortByPipe,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import {AccordionComponent, DynamicHeadingComponent, IconComponent} from "@vp/shared-ui-components";

@NgModule({
	imports: [
		CommonModule,
		TranslateModule,
		UiDirectivesModule,
		RouterLink,
		ReactiveFormsModule,
		TranslateTextPipe,
		MarkdownPipe,
		SortByPipe,
		ConcatPipe,
		DynamicHeadingComponent,
		IconComponent,
		AccordionComponent,
	],
	declarations: [
		BackendErrorComponent,
		ClearableInputComponent,
		FooterComponentComponent,
		AnswerComponent,
		AnswerCandidateComponent,
		AnswerListComponent,
		AccordionVoteComponent,
		AccordionElectionComponent,
		ModalInvalidCodesComponent,
		FinalizationCodeComponent
	],
	exports: [
		BackendErrorComponent,
		ClearableInputComponent,
		FooterComponentComponent,
		AnswerComponent,
		AnswerCandidateComponent,
		AnswerListComponent,
		AccordionVoteComponent,
		AccordionElectionComponent,
		ModalInvalidCodesComponent,
		FinalizationCodeComponent
	],
})
export class UiComponentsModule {
}
