/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
	MockElectionInformation,
	MockEmptyList,
	MockList,
	mockTranslateText,
	RandomItem,
	RandomString,
} from '@vp/shared-util-testing';
import { ShortChoiceReturnCode } from '@vp/voter-portal-util-types';
import { MockComponent, MockPipe, MockProvider } from 'ng-mocks';
import { Store } from '@ngrx/store';
import { signal } from '@angular/core';
import {
	ElectionInformationAnswers,
	TranslateTextPipe,
} from 'e-voting-libraries-ui-kit';
import { TranslatePipe } from '@ngx-translate/core';
import { AnswerComponent } from '@vp/voter-portal-ui-components';
import { AnswerListComponent } from './answer-list.component';
import {IconComponent} from "@vp/shared-ui-components";

describe('AnswerListComponent', () => {
	let component: AnswerListComponent;
	let fixture: ComponentFixture<AnswerListComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [MockComponent(IconComponent)],
			declarations: [
				AnswerListComponent,
				MockComponent(AnswerComponent),
				MockPipe(TranslateTextPipe, mockTranslateText),
				MockPipe(TranslatePipe, (value) => value),
			],
			providers: [
				MockProvider(Store, {
					selectSignal: jest.fn().mockReturnValue(signal(undefined)),
				} as unknown as Store),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(AnswerListComponent);
		component = fixture.componentInstance;

		component.electionInformation = MockElectionInformation({ hasLists: true });

		fixture.detectChanges();
	});

	function setElectionInformationAnswers(value: object = {}) {
		component.electionInformationAnswers =
			value as unknown as ElectionInformationAnswers;
	}

	it('should initialize list in ngAfterContentInit', () => {
		const list = RandomItem(component.electionInformation.lists);
		setElectionInformationAnswers({
			chosenList: { listIdentification: list.listIdentification },
		});

		component.ngAfterContentInit();

		expect(component.list).toEqual(list);
	});

	it('should render no title content when isAnswerUnknown is true', () => {
		component.list = MockList();

		fixture.detectChanges();

		const listTitleElement = fixture.nativeElement.querySelector(
			'[data-test="list-title"]',
		);
		expect(listTitleElement).toBeNull();
	});

	describe('has answer', () => {
		beforeEach(() => {
			setElectionInformationAnswers();
		});

		it('should render list description when list is set', () => {
			const displayListLine1 = RandomString();
			component.list = MockList({ displayListLine1 });

			fixture.detectChanges();

			const listTitleElement = fixture.nativeElement.querySelector(
				'[data-test="list-title"]',
			);
			expect(listTitleElement.textContent).toContain(displayListLine1);
		});

		it('should render empty list description text when no list', () => {
			const listDescription = RandomString();
			component.electionInformation.emptyList = MockEmptyList({
				listDescription,
			});

			fixture.detectChanges();

			const listTitleElement = fixture.nativeElement.querySelector(
				'[data-test="list-title"]',
			);
			expect(listTitleElement.textContent).toContain(listDescription);
		});
	});

	describe('short choice return code', () => {
		it('should set listShortChoiceReturnCode in ngAfterContentInit', () => {
			component.ngAfterContentInit();
			expect(component.listShortChoiceReturnCode).toBeDefined();
		});

		it('should render the shortChoiceReturnCode when it is available', () => {
			const shortChoiceReturnCode = RandomString(4, '0123456789');
			component.listShortChoiceReturnCode = signal({
				shortChoiceReturnCode,
			} as ShortChoiceReturnCode);

			fixture.detectChanges();

			const renderedCode = fixture.nativeElement.querySelector(
				'.shortChoiceReturnCode',
			);
			expect(renderedCode.textContent).toContain(shortChoiceReturnCode);
			expect(renderedCode.textContent).toContain('verify.main.yourchoicecode');
		});
	});
});
