/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { VerifyVoteQuestionComponent } from './verify-vote-question.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
	MarkdownPipe,
	QuestionBase,
	StandardAnswer,
	TieBreakAnswer,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import { MockComponent, MockPipe, MockProvider } from 'ng-mocks';
import {
	MockStandardAnswer,
	MockStandardQuestion,
	mockTranslateText,
} from '@vp/shared-util-testing';
import { TranslateTestingModule } from 'ngx-translate-testing';
import { Store } from '@ngrx/store';
import { Signal, signal } from '@angular/core';
import { AnswerComponent } from '@vp/voter-portal-ui-components';
import {IconComponent} from "@vp/shared-ui-components";

describe('VerifyVoteQuestionComponent', () => {
	type TestAnswerType = StandardAnswer | TieBreakAnswer;
	type TestQuestionType = QuestionBase<TestAnswerType>;

	let component: VerifyVoteQuestionComponent<TestQuestionType, TestAnswerType>;
	let fixture: ComponentFixture<
		VerifyVoteQuestionComponent<TestQuestionType, TestAnswerType>
	>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				VerifyVoteQuestionComponent,
				MockComponent(AnswerComponent),
				MockPipe(TranslateTextPipe, mockTranslateText),
				MockPipe(MarkdownPipe),
			],
			imports: [TranslateTestingModule.withTranslations({}), MockComponent(IconComponent)],
			providers: [
				MockProvider(Store, {
					selectSignal: <K>() => signal(<K>null) as Signal<K>,
				}),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(VerifyVoteQuestionComponent);
		component = fixture.componentInstance;

		component.shortChoiceReturnCode = '1234';
		component.question = MockStandardQuestion();

		fixture.detectChanges();
	});

	it('should pass the provided question to the question component', () => {
		expect(fixture.debugElement.componentInstance.question).toBe(
			component.question,
		);
	});

	it('should show translated answer information within the question component', () => {
		component.shortChoiceReturnCode = '1234';
		component.chosenAnswer = MockStandardAnswer();
		fixture.detectChanges();

		expect(fixture.debugElement.nativeElement.textContent).toContain(
			component.chosenAnswer?.answerInformation.DE,
		);
	});

	it('should show short choice codes within the question component', () => {
		component.shortChoiceReturnCode = '1234';
		fixture.detectChanges();

		expect(fixture.debugElement.nativeElement.textContent).toContain(
			component.shortChoiceReturnCode,
		);
	});

	it('should show screenreader message "common.screenreader.nooptionselected" if empty answer is selected', () => {
		component.shortChoiceReturnCode = '1234';
		component.chosenAnswer = MockStandardAnswer({ isEmptyAnswer: true });
		component.isEmptyAnswer = true;
		fixture.detectChanges();

		expect(fixture.debugElement.nativeElement.textContent).toContain(
			'common.screenreader.nooptionselected',
		);
	});
});
