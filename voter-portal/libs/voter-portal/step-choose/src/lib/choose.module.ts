/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ChooseComponent } from './choose/choose.component';
import { ChooseElectionGroupComponent } from './choose-election-group/choose-election-group.component';
import { ChooseElectionComponent } from './choose-election/choose-election.component';
import { ChooseElectionListComponent } from './choose-election-list/choose-election-list.component';
import { ChooseElectionCandidateComponent } from './choose-election-candidate/choose-election-candidate.component';
import { ChooseVoteComponent } from './choose-vote/choose-vote.component';
import { ChooseVoteQuestionComponent } from './choose-vote-question/choose-vote-question.component';
import { UiComponentsModule } from '@vp/voter-portal-ui-components';
import { TranslateModule } from '@ngx-translate/core';
import { UiDirectivesModule } from '@vp/voter-portal-ui-directives';
import { ModalCandidateComponent } from './modal-candidate/modal-candidate.component';
import { ModalCandidateSelectorComponent } from './modal-candidate-selector/modal-candidate-selector.component';
import { ModalCandidateSelectorEnabledComponent } from './modal-candidate-selector-enabled/modal-candidate-selector-enabled.component';
import { ModalCandidateSelectorDisabledComponent } from './modal-candidate-selector-disabled/modal-candidate-selector-disabled.component';
import { ModalListSelectorComponent } from './modal-list-selector/modal-list-selector.component';
import { NgbCollapseModule } from '@ng-bootstrap/ng-bootstrap';
import { ModalListComponent } from './modal-list/modal-list.component';
import { ModalCandidateWriteInSelectorComponent } from './modal-candidate-write-in-selector/modal-candidate-write-in-selector.component';
import { ChooseElectionCandidateWriteInComponent } from './choose-election-candidate-write-in/choose-election-candidate-write-in.component';
import {
	ConcatPipe,
	MarkdownPipe,
	SortByPipe,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import {IconComponent} from "@vp/shared-ui-components";

@NgModule({
	imports: [
		CommonModule,
		ReactiveFormsModule,
		UiComponentsModule,
		TranslateModule,
		UiDirectivesModule,
		FormsModule,
		NgbCollapseModule,
		TranslateTextPipe,
		SortByPipe,
		MarkdownPipe,
		ConcatPipe,
		IconComponent,
	],
	declarations: [
		ChooseComponent,
		ChooseElectionGroupComponent,
		ChooseElectionComponent,
		ChooseElectionListComponent,
		ChooseElectionCandidateComponent,
		ChooseElectionCandidateWriteInComponent,
		ChooseVoteComponent,
		ChooseVoteQuestionComponent,
		ModalCandidateComponent,
		ModalCandidateSelectorComponent,
		ModalCandidateSelectorEnabledComponent,
		ModalCandidateSelectorDisabledComponent,
		ModalCandidateWriteInSelectorComponent,
		ModalListComponent,
		ModalListSelectorComponent,
	],
})
export class ChooseModule {}
