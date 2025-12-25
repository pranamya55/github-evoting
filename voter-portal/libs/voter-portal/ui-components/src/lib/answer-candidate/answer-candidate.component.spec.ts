/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslatePipe } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { MockComponent, MockPipe, MockProvider } from 'ng-mocks';
import { CandidateShortChoiceReturnCode } from '@vp/voter-portal-util-types';
import {
	ElectionInformationAnswers,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import {
	MockCandidate,
	MockElectionInformation,
	mockTranslateText,
	RandomArray,
	RandomItem,
	RandomString,
} from '@vp/shared-util-testing';
import {
	AnswerCandidateComponent,
	AnswerComponent,
} from '@vp/voter-portal-ui-components';
import {DynamicHeadingComponent, IconComponent} from "@vp/shared-ui-components";


describe('AnswerCandidateComponent', () => {
	let component: AnswerCandidateComponent;
	let fixture: ComponentFixture<AnswerCandidateComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [MockComponent(IconComponent)],
			declarations: [
				AnswerCandidateComponent,
				MockComponent(AnswerComponent),
				MockComponent(DynamicHeadingComponent),
				MockPipe(TranslateTextPipe, mockTranslateText),
				MockPipe(TranslatePipe, (value) => value),
			],
			providers: [
				MockProvider(Store, {
					selectSignal: jest.fn().mockReturnValue(signal(undefined)),
				} as unknown as Store),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(AnswerCandidateComponent);
		component = fixture.componentInstance;

		const mockElectionInformation = MockElectionInformation();
		component.electionInformation = mockElectionInformation;
		component.emptyPosition = RandomItem(
			mockElectionInformation.emptyList.emptyPositions,
		);

		fixture.detectChanges();
	});

	function setElectionInformationAnswers(value?: object) {
		component.electionInformationAnswers = {
			chosenCandidates: [],
			...value,
		} as unknown as ElectionInformationAnswers;
	}

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should set isPositionEmpty correctly', () => {
		expect(component.isPositionEmpty).toBeUndefined();

		setElectionInformationAnswers();
		expect(component.isPositionEmpty).toBe(true);

		component.writeIn = RandomString();
		expect(component.isPositionEmpty).toBe(false);

		component.writeIn = undefined;
		component.candidate = MockCandidate();
		expect(component.isPositionEmpty).toBe(false);
	});

	it('should initialize writeIn in ngAfterContentInit', () => {
		const positionOnList = component.emptyPosition.positionOnList;
		const writeIns = RandomArray(
			(i) => `writeIn-${i}`,
			positionOnList,
			positionOnList,
		);
		setElectionInformationAnswers({
			chosenWriteIns: writeIns.map((writeIn) => ({ writeIn })),
		});

		component.ngAfterContentInit();

		expect(component.writeIn).toEqual(writeIns[positionOnList - 1]);
		expect(component.candidate).toBeUndefined();
	});

	it('should initialize candidate in ngAfterContentInit', () => {
		const positionOnList = component.emptyPosition.positionOnList;
		const candidates = RandomArray(
			() => RandomItem(component.electionInformation.candidates),
			positionOnList,
			positionOnList,
		);
		setElectionInformationAnswers({
			chosenCandidates: candidates.map(({ candidateIdentification }) => ({
				candidateIdentification,
			})),
		});

		component.ngAfterContentInit();

		expect(component.candidate).toEqual(candidates[positionOnList - 1]);
		expect(component.writeIn).toBeUndefined();
		expect(component.isPositionEmpty).toBe(false);
	});

	it('should render render no title content when isAnswerUnknown is true', () => {
		component.candidate = MockCandidate();
		component.writeIn = RandomString();

		fixture.detectChanges();

		const candidateTitleElement =
			fixture.nativeElement.querySelector('vp-dynamic-heading');
		expect(candidateTitleElement.textContent).toContain(
			'listandcandidates.selection',
		);
	});

	describe('has answer', () => {
		beforeEach(() => {
			setElectionInformationAnswers();
		});

		it('should render candidate information when candidate is set', () => {
			const displayCandidateLine1 = RandomString();
			const displayCandidateLine4 = RandomString();
			component.candidate = MockCandidate({
				displayCandidateLine1,
				displayCandidateLine4,
			});

			fixture.detectChanges();

			const candidateLine4Element =
				fixture.nativeElement.querySelector('p.fs-small');
			expect(candidateLine4Element.textContent).toContain(
				displayCandidateLine4,
			);

			const candidateTitleElement =
				fixture.nativeElement.querySelector('vp-dynamic-heading');
			expect(candidateTitleElement.textContent).toContain(
				displayCandidateLine1,
			);
		});

		it('should render write-in when available', () => {
			component.writeIn = RandomString();

			fixture.detectChanges();

			const candidateTitleElement =
				fixture.nativeElement.querySelector('vp-dynamic-heading');
			expect(candidateTitleElement.textContent).toContain(component.writeIn);
		});

		it('should render empty position text when no candidate or write-in', () => {
			component.candidate = undefined;
			component.writeIn = undefined;

			fixture.detectChanges();

			const candidateTitleElement =
				fixture.nativeElement.querySelector('vp-dynamic-heading');
			expect(candidateTitleElement.textContent).toContain(
				component.emptyPosition.emptyPositionText.DE,
			);
		});
	});

	describe('short choice return code', () => {
		it('should set candidateShortChoiceReturnCode in ngAfterContentInit', () => {
			component.ngAfterContentInit();
			expect(component.candidateShortChoiceReturnCode).toBeDefined();
		});

		it('should render the shortChoiceReturnCode when it is available', () => {
			const shortChoiceReturnCode = RandomString(4, '0123456789');
			component.candidateShortChoiceReturnCode = signal({
				shortChoiceReturnCode,
			} as CandidateShortChoiceReturnCode);

			fixture.detectChanges();

			const renderedCode = fixture.nativeElement.querySelector(
				'.shortChoiceReturnCode',
			);
			expect(renderedCode.textContent).toContain(shortChoiceReturnCode);
			expect(renderedCode.textContent).toContain('verify.main.yourchoicecode');
		});
	});
});
