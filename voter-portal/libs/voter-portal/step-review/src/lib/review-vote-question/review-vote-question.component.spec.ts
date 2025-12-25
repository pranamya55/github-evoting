/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReviewVoteQuestionComponent } from './review-vote-question.component';
import {
	ControlContainer,
	FormArray,
	FormBuilder,
	ReactiveFormsModule,
} from '@angular/forms';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import {
	MarkdownPipe,
	QuestionBase,
	StandardAnswer,
	TieBreakAnswer,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import { Store } from '@ngrx/store';
import { Signal, signal } from '@angular/core';
import { AnswerComponent } from '@vp/voter-portal-ui-components';

describe('ReviewVoteQuestionComponent', () => {
	type TestAnswerType = StandardAnswer | TieBreakAnswer;
	type TestQuestionType = QuestionBase<TestAnswerType>;

	let component: ReviewVoteQuestionComponent<TestQuestionType, TestAnswerType>;
	let fixture: ComponentFixture<
		ReviewVoteQuestionComponent<TestQuestionType, TestAnswerType>
	>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ReviewVoteQuestionComponent,
				MockComponent(AnswerComponent),
				MockPipe(MarkdownPipe),
				MockPipe(TranslateTextPipe),
			],
			imports: [MockModule(ReactiveFormsModule)],
			providers: [
				FormBuilder,
				MockProvider(ControlContainer, {
					control: new FormArray([]),
				}),
				MockProvider(Store, {
					selectSignal: <K>() => signal(<K>null) as Signal<K>,
				}),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(
			ReviewVoteQuestionComponent<TestQuestionType, TestAnswerType>,
		);
		component = fixture.componentInstance;

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
