/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ChooseVoteQuestionComponent } from './choose-vote-question.component';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import {
	MarkdownPipe,
	QuestionBase,
	SortByPipe,
	StandardAnswer,
	TieBreakAnswer,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import { AnswerComponent } from '@vp/voter-portal-ui-components';
import { TranslateModule } from '@ngx-translate/core';
import { ChooseElectionComponent } from '../choose-election/choose-election.component';
import { ChooseFormService } from '../choose-form.service';
import {IconComponent} from "@vp/shared-ui-components";

const MockChooseFormService = {
	createChosenAnswerFormGroup: jest.fn().mockReturnValue(
		new FormGroup({
			answerIdentification: new FormControl(),
		}),
	),
};

describe('ChooseVoteQuestionComponent', () => {
	type TestAnswerType = StandardAnswer | TieBreakAnswer;
	type TestQuestionType = QuestionBase<TestAnswerType>;

	let component: ChooseVoteQuestionComponent<TestQuestionType, TestAnswerType>;
	let fixture: ComponentFixture<
		ChooseVoteQuestionComponent<TestQuestionType, TestAnswerType>
	>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ChooseVoteQuestionComponent,
				MockComponent(AnswerComponent),
				MockPipe(MarkdownPipe),
				MockPipe(SortByPipe),
				MockPipe(TranslateTextPipe),
			],
			imports: [ReactiveFormsModule, MockModule(TranslateModule), MockComponent(IconComponent),],
		})
			.overrideComponent(ChooseElectionComponent, {
				set: {
					providers: [MockProvider(ChooseFormService, MockChooseFormService)],
				},
			})
			.compileComponents();

		fixture = TestBed.createComponent(
			ChooseVoteQuestionComponent<TestQuestionType, TestAnswerType>,
		);
		component = fixture.componentInstance;

		component.voteIdentification = 'test-vote-id';
		component.ballotIdentification = 'test-ballot-id';
		component.question = {
			questionIdentification: 'test-question-id',
			ballotQuestion: {},
			answers: [],
		} as unknown as TestQuestionType;

		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
