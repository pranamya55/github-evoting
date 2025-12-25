/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { UiComponentsModule } from '@vp/voter-portal-ui-components';
import { VerifyComponent } from './verify/verify.component';
import { UiDirectivesModule } from '@vp/voter-portal-ui-directives';
import { VerifyVoteQuestionComponent } from './verify-vote-question/verify-vote-question.component';
import { VerifyElectionGroupComponent } from './verify-election-group/verify-election-group.component';
import { VerifyElectionComponent } from './verify-election/verify-election.component';
import { RouterLink } from '@angular/router';
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
		TranslateModule,
		UiComponentsModule,
		UiDirectivesModule,
		RouterLink,
		ConcatPipe,
		SortByPipe,
		TranslateTextPipe,
		MarkdownPipe,
		IconComponent,
	],
	declarations: [
		VerifyComponent,
		VerifyVoteQuestionComponent,
		VerifyElectionGroupComponent,
		VerifyElectionComponent,
	],
})
export class VerifyModule {}
