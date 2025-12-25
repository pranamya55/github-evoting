/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MockComponent, MockPipe} from 'ng-mocks';
import {Component} from '@angular/core';
import {MockElectionInformation, mockSortBy, mockTranslateText, RandomInt,} from '@vp/shared-util-testing';
import {AccordionElectionComponent} from './accordion-election.component';
import {By} from '@angular/platform-browser';
import {TranslatePipe} from '@ngx-translate/core';
import {ElectionInformation, SortByPipe, TranslateTextPipe} from 'e-voting-libraries-ui-kit';
import {AccordionComponent, DynamicHeadingComponent} from "@vp/shared-ui-components";
import {NgTemplateOutlet} from "@angular/common";

@Component({
	template: `
		<vp-accordion-election
				[electionInformation]="electionInformation"
				[chosenCandidateCount]="chosenCandidateCount"
		>
			<ng-template #listTemplate>
				<span data-test="list-template"></span>
			</ng-template>
			<ng-template #candidateTemplate let-emptyPosition="emptyPosition">
				<div data-test="candidate-template">
					{{ emptyPosition.emptyPositionIdentification }}
				</div>
			</ng-template>
		</vp-accordion-election>
	`,
	standalone: false,
})
class TestingHost {
	electionInformation: ElectionInformation = MockElectionInformation({hasLists: true});
	chosenCandidateCount?: number;
}

describe('AccordionElectionComponent', () => {
	let component: TestingHost;
	let fixture: ComponentFixture<TestingHost>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				TestingHost,
				AccordionElectionComponent,
				MockComponent(DynamicHeadingComponent),
				MockComponent(AccordionComponent),
				MockPipe(
					TranslatePipe,
					(key: string, params: { [key: string]: unknown }) =>
						`${key} ${params ? Object.values(params).join(' ') : ''}`,
				),
				MockPipe(TranslateTextPipe, mockTranslateText),
				MockPipe(SortByPipe, mockSortBy),
			],
			imports: [NgTemplateOutlet]
		}).compileComponents();

		fixture = TestBed.createComponent(TestingHost);
		component = fixture.componentInstance;

		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should show the election description', () => {
		const heading: HTMLElement = fixture.debugElement.query(
			By.css('.h6'),
		).nativeElement;
		const electionDescription =
			component.electionInformation.election.electionDescription.DE;
		expect(heading.textContent).toContain(electionDescription);
	});

	it('should show the chosen candidate count when provided', () => {
		const numberOfMandates =
			component.electionInformation.election.numberOfMandates;
		component.chosenCandidateCount = RandomInt(numberOfMandates);

		fixture.detectChanges();

		const title: HTMLElement = fixture.debugElement.query(
			By.css('[data-test="title"]'),
		).nativeElement;
		expect(title.textContent).toContain(String(numberOfMandates));
		expect(title.textContent).toContain(String(component.chosenCandidateCount));
	});

	it('should render an ordered list of candidates', () => {
		const candidates = fixture.debugElement.queryAll(
			By.css('ol.candidates > li.candidate'),
		);
		expect(candidates.length).toBe(
			component.electionInformation.emptyList.emptyPositions.length,
		);
	});

	it('should not render a list when the election has none', () => {
		component.electionInformation.lists = [];

		fixture.detectChanges();

		const listTemplate = fixture.debugElement.query(
			By.css('[data-test="list-template"]'),
		);
		expect(listTemplate).toBeNull();
	});

	it('should render a list when the election has some', () => {
		component.electionInformation = MockElectionInformation({hasLists: true});

		fixture.detectChanges();

		const listTemplate = fixture.debugElement.query(
			By.css('[data-test="list-template"]'),
		);
		expect(listTemplate).toBeTruthy();
	});

	it('should correctly display the candidate templates', () => {
		const emptyPositions = component.electionInformation.emptyList.emptyPositions;
		const candidateTemplates = fixture.debugElement.queryAll(
			By.css('[data-test="candidate-template"]'),
		);

		expect(candidateTemplates.length).toBe(emptyPositions.length);

		candidateTemplates.forEach((candidate, i) => {
			const emptyPosition =
				component.electionInformation.emptyList.emptyPositions[i];
			expect(candidate.nativeElement.textContent).toContain(
				emptyPosition.emptyPositionIdentification,
			);
		});
	});
});
