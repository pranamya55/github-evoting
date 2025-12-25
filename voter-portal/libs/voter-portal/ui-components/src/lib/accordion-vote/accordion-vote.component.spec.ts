/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {Component, DebugElement} from '@angular/core';
import {By} from '@angular/platform-browser';
import {NgbAccordionModule} from '@ng-bootstrap/ng-bootstrap';
import {TranslatePipe} from '@ngx-translate/core';
import {MockComponent, MockPipe} from 'ng-mocks';
import {mockConcat, mockSortBy, mockTranslateText, MockVoteTexts,} from '@vp/shared-util-testing';
import {
	ConcatPipe,
	SortByPipe,
	StandardBallot,
	StandardQuestion,
	TieBreakQuestion,
	TranslateTextPipe,
	VariantBallot,
} from 'e-voting-libraries-ui-kit';
import {AccordionVoteComponent} from './accordion-vote.component';
import {AccordionComponent} from "@vp/shared-ui-components";

const standardBallot = {
	ballotIdentification: 'standard-ballot',
	questionNumber: '1',
	questionIdentification: 'standard-ballot-question',
} as StandardBallot;

const standardQuestion = {
	questionNumber: '2a',
	questionIdentification: 'variant-ballot-standard-question',
} as StandardQuestion;

const tieBreakQuestion = {
	questionNumber: '2b',
	questionIdentification: 'variant-ballot-tie-break-question',
} as TieBreakQuestion;

const variantBallot = {
	ballotIdentification: 'variant-ballot',
	standardQuestions: [standardQuestion],
	tieBreakQuestions: [tieBreakQuestion],
} as VariantBallot;

@Component({
	template: `
		<vp-accordion-vote [voteTexts]="voteTexts">
			<ng-template
				let-ballotIdentification="ballotIdentification"
				let-question="question"
			>
				<div data-test="question-template">
					{{ question.questionIdentification }} {{ ballotIdentification }}
				</div>
			</ng-template>
		</vp-accordion-vote>
	`,
	standalone: false,
})
class TestingHost {
	voteTexts = MockVoteTexts();

	constructor() {
		this.voteTexts.ballots = [standardBallot, variantBallot];
	}
}

describe('AccordionVoteComponent', () => {
	let component: TestingHost;
	let fixture: ComponentFixture<TestingHost>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				TestingHost,
				AccordionVoteComponent,
				MockComponent(AccordionComponent),
				MockPipe(TranslatePipe),
				MockPipe(TranslateTextPipe, mockTranslateText),
				MockPipe(ConcatPipe, mockConcat),
				MockPipe(SortByPipe, mockSortBy),
			],
			imports: [NgbAccordionModule],
		}).compileComponents();

		fixture = TestBed.createComponent(TestingHost);
		component = fixture.componentInstance;

		fixture.detectChanges();
	});

	function getQuestionNumber(listItem: DebugElement) {
		return listItem.query(By.css('strong')).nativeElement.textContent;
	}

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should render an ordered list per vote', () => {
		const voteList = fixture.debugElement.queryAll(By.css('ol.vote'));
		expect(voteList.length).toBe(1);
	});

	it('should render standard ballots as list items', () => {
		const standardBallotListItems = fixture.debugElement.queryAll(
			By.css('.vote > li.standard-ballot'),
		);
		expect(standardBallotListItems.length).toBe(1);
	});

	it('should correctly set the question number on the standard ballot list items', () => {
		const standardBallotListItem = fixture.debugElement.query(
			By.css('.vote > li.standard-ballot'),
		);
		expect(getQuestionNumber(standardBallotListItem)).toBe(
			`${standardBallot.questionNumber}.`,
		);
	});

	it('should render variant ballots as nested ordered lists', () => {
		const variantBallotLists = fixture.debugElement.queryAll(
			By.css('.vote > li > ol.variant-ballot'),
		);
		expect(variantBallotLists.length).toBe(1);
	});

	it('should correctly set the question number on the variant ballot list items', () => {
		const variantBallotListItems = fixture.debugElement.queryAll(
			By.css('.vote > li > ol.variant-ballot > li'),
		);
		expect(variantBallotListItems.length).toBe(2);
		expect(getQuestionNumber(variantBallotListItems[0])).toBe(
			`${standardQuestion.questionNumber}.`,
		);
		expect(getQuestionNumber(variantBallotListItems[1])).toBe(
			`${tieBreakQuestion.questionNumber}.`,
		);
	});

	it('should correctly display the question templates', () => {
		const questions = [standardBallot, standardQuestion, tieBreakQuestion];
		const questionTemplates = fixture.debugElement.queryAll(
			By.css('[data-test="question-template"]'),
		);
		expect(questionTemplates.length).toBe(questions.length);
		questions.forEach((question, i) => {
			expect(questionTemplates[i].nativeElement.textContent).toContain(
				question.questionIdentification,
			);
			expect(questionTemplates[i].nativeElement.textContent).toContain(
				'ballotIdentification' in question
					? standardBallot.ballotIdentification
					: variantBallot.ballotIdentification,
			);
		});
	});
});
