/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
	ControlContainer,
	FormArray,
	FormControl,
	FormGroup,
	ReactiveFormsModule,
} from '@angular/forms';
import { MockProvider } from 'ng-mocks';
import { ChooseVoteComponent } from './choose-vote.component';
import { By } from '@angular/platform-browser';
import { StandardQuestion, VoteTexts } from 'e-voting-libraries-ui-kit';
import { ChooseFormService } from '../choose-form.service';
import {
	MockStandardQuestion,
	MockVoteTexts,
	RandomString,
} from '@vp/shared-util-testing';
import {
	Component,
	ContentChild,
	inject,
	Input,
	TemplateRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
	selector: 'vp-accordion-vote',
	template: `
		<ng-container
			*ngTemplateOutlet="t; context: { question, ballotIdentification }"
		/>
	`,
	standalone: false,
})
class MockAccordionVoteComponent {
	@ContentChild(TemplateRef) t!: TemplateRef<{
		question: object;
		ballotIdentification: string;
	}>;
	question = MockStandardQuestion();
	ballotIdentification = RandomString();

	@Input() formGroup?: FormGroup;
	@Input() voteTexts?: VoteTexts;
}

@Component({
	selector: 'vp-choose-vote-question',
	template: `<input type="text" [formControl]="formControl" />`,
	standalone: false,
})
class MockVoteQuestionComponent {
	formControl = new FormControl();
	@Input() question?: StandardQuestion;
	@Input() ballotIdentification?: string;
	@Input() voteIdentification?: string;
	private readonly controlContainer = inject(ControlContainer);

	constructor() {
		const parentControl = this.controlContainer.control as FormArray;
		parentControl.push(this.formControl);
	}
}

describe('ChooseVoteComponent', () => {
	let component: ChooseVoteComponent;
	let fixture: ComponentFixture<ChooseVoteComponent>;
	let voteAnswer: FormGroup;
	let chooseFormService: ChooseFormService;

	beforeEach(async () => {
		voteAnswer = new FormGroup({
			chosenAnswers: new FormArray([]),
		});
		chooseFormService = {
			createVoteAnswersFormGroup: jest.fn().mockReturnValue(voteAnswer),
		} as unknown as ChooseFormService;

		await TestBed.configureTestingModule({
			declarations: [
				ChooseVoteComponent,
				MockAccordionVoteComponent,
				MockVoteQuestionComponent,
			],
			imports: [CommonModule, ReactiveFormsModule],
		})
			.overrideComponent(ChooseVoteComponent, {
				set: {
					providers: [MockProvider(ChooseFormService, chooseFormService)],
				},
			})
			.compileComponents();

		fixture = TestBed.createComponent(ChooseVoteComponent);
		component = fixture.componentInstance;

		component.voteTexts = MockVoteTexts();

		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should create the voteAnswer form group', () => {
		expect(chooseFormService.createVoteAnswersFormGroup).toHaveBeenCalledTimes(
			1,
		);
		expect(component.voteAnswer).toEqual(voteAnswer);
	});

	it('should render a vote accordion with the correct input', () => {
		const voteAccordion = fixture.debugElement.query(
			By.css('vp-accordion-vote'),
		);
		expect(voteAccordion.componentInstance.voteTexts).toEqual(
			component.voteTexts,
		);
		expect(voteAccordion.componentInstance.formGroup).toEqual(
			component.voteAnswer,
		);
	});

	it('should render a vote question with the correct input', () => {
		const { question, ballotIdentification } = fixture.debugElement.query(
			By.css('vp-accordion-vote'),
		).componentInstance;
		const voteQuestion = fixture.debugElement.query(
			By.css('vp-choose-vote-question'),
		);
		expect(voteQuestion.componentInstance.question).toEqual(question);
		expect(voteQuestion.componentInstance.ballotIdentification).toEqual(
			ballotIdentification,
		);
		expect(voteQuestion.componentInstance.voteIdentification).toEqual(
			component.voteTexts.voteIdentification,
		);
	});

	it('should add the form control from the vote question to the voteAnswer', () => {
		const { formControl } = fixture.debugElement.query(
			By.css('vp-choose-vote-question'),
		).componentInstance;
		expect(component.voteAnswer.controls.chosenAnswers.controls).toContain(
			formControl,
		);
	});
});
