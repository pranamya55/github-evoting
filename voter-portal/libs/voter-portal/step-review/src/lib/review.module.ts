/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { UiComponentsModule } from '@vp/voter-portal-ui-components';
import { ReviewComponent } from './review/review.component';
import { ReviewVoteQuestionComponent } from './review-vote-question/review-vote-question.component';
import { ReviewElectionGroupComponent } from './review-election-group/review-election-group.component';
import { ReviewElectionComponent } from './review-election/review-election.component';
import { SendVoteModalComponent } from './send-vote-modal/send-vote-modal.component';
import { UiDirectivesModule } from '@vp/voter-portal-ui-directives';
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
		ReviewComponent,
		ReviewVoteQuestionComponent,
		ReviewElectionGroupComponent,
		ReviewElectionComponent,
		SendVoteModalComponent,
	],
})
export class ReviewModule {}
