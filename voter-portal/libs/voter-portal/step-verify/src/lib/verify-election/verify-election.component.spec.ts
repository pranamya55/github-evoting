/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
	Component,
	ContentChildren,
	Input,
	signal,
	TemplateRef,
} from '@angular/core';
import { By } from '@angular/platform-browser';
import { Store } from '@ngrx/store';
import { MockComponent, MockProvider } from 'ng-mocks';
import { TranslateTestingModule } from 'ngx-translate-testing';
import {
	ElectionInformation,
	ElectionInformationAnswers,
} from 'e-voting-libraries-ui-kit';
import {
	MockElectionInformation,
	MockElectionInformationAnswers,
	MockEmptyPosition,
	RandomString,
} from '@vp/shared-util-testing';
import {
	AnswerCandidateComponent,
	AnswerListComponent,
} from '@vp/voter-portal-ui-components';
import { VerifyElectionComponent } from './verify-election.component';

@Component({
	selector: 'vp-accordion-election',
	template: `
		@for (t of templates; track $index) {
			<ng-container *ngTemplateOutlet="t; context: { emptyPosition }" />
		}
	`,
	standalone: false,
})
class MockAccordionElectionComponent {
	@ContentChildren(TemplateRef) templates: TemplateRef<any>[] = [];
	emptyPosition = MockEmptyPosition();

	@Input() electionInformation?: ElectionInformation;
	@Input() headingLevel?: number;
}

describe('VerifyElectionComponent', () => {
	let component: VerifyElectionComponent;
	let fixture: ComponentFixture<VerifyElectionComponent>;
	let electionInformationAnswers: ElectionInformationAnswers;
	let store: Store;

	beforeEach(async () => {
		electionInformationAnswers = MockElectionInformationAnswers();
		store = {
			selectSignal: jest
				.fn()
				.mockReturnValue(signal(electionInformationAnswers)),
		} as unknown as Store;

		await TestBed.configureTestingModule({
			declarations: [
				VerifyElectionComponent,
				MockAccordionElectionComponent,
				MockComponent(AnswerListComponent),
				MockComponent(AnswerCandidateComponent),
			],
			imports: [TranslateTestingModule.withTranslations({})],
			providers: [MockProvider(Store, store)],
		}).compileComponents();

		fixture = TestBed.createComponent(VerifyElectionComponent);
		component = fixture.componentInstance;

		component.electionGroupIdentification = RandomString();
		component.electionInformation = MockElectionInformation();

		fixture.detectChanges();
	});

	it('should get the electionInformationAnswers form the store', () => {
		expect(store.selectSignal).toHaveBeenCalledTimes(1);
		expect(component.electionInformationAnswers).toEqual(
			electionInformationAnswers,
		);
	});

	it('should render an election accordion with the correct inputs', () => {
		const electionAccordion = fixture.debugElement.query(
			By.css('vp-accordion-election'),
		);
		expect(electionAccordion).toBeTruthy();
		expect(electionAccordion.componentInstance.electionInformation).toEqual(
			component.electionInformation,
		);
	});

	it('should pass expected inputs to the list component', () => {
		const listChoice = fixture.debugElement.query(By.css('vp-answer-list'));
		expect(listChoice).toBeTruthy();
		expect(listChoice.componentInstance.electionInformation).toEqual(
			component.electionInformation,
		);
		expect(listChoice.componentInstance.electionInformationAnswers).toEqual(
			electionInformationAnswers,
		);
	});

	it('should pass expected inputs to the candidate component', () => {
		const { emptyPosition } = fixture.debugElement.query(
			By.css('vp-accordion-election'),
		).componentInstance;
		const candidateChoice = fixture.debugElement.query(
			By.css('vp-answer-candidate'),
		);
		expect(candidateChoice).toBeTruthy();
		expect(candidateChoice.componentInstance.electionInformation).toEqual(
			component.electionInformation,
		);
		expect(
			candidateChoice.componentInstance.electionInformationAnswers,
		).toEqual(electionInformationAnswers);
		expect(candidateChoice.componentInstance.emptyPosition).toEqual(
			emptyPosition,
		);
	});
});
