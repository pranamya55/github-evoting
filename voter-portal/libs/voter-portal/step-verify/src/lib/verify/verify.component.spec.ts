/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { VerifyComponent } from './verify.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import { VerifyElectionGroupComponent } from '../verify-election-group/verify-election-group.component';
import { ConcatPipe, SortByPipe } from 'e-voting-libraries-ui-kit';
import {
	mockConcat,
	mockSortBy,
	MockStoreProvider,
	setState,
} from '@vp/shared-util-testing';
import {
	AccordionVoteComponent,
	ModalInvalidCodesComponent,
	UiComponentsModule,
} from '@vp/voter-portal-ui-components';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { MockStore } from '@ngrx/store/testing';
import { FAQService } from '@vp/voter-portal-feature-faq';
import { TranslateTestingModule } from 'ngx-translate-testing';
import { FAQSection } from '@vp/voter-portal-util-types';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { VerifyVoteQuestionComponent } from '../verify-vote-question/verify-vote-question.component';
import { IconComponent } from "@vp/shared-ui-components";

describe('VerifyComponent', () => {
	let component: VerifyComponent;
	let fixture: ComponentFixture<VerifyComponent>;
	let faqService: FAQService;
	let store: MockStore;
	let modal: NgbModal;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				VerifyComponent,
				MockComponent(AccordionVoteComponent),
				MockComponent(VerifyVoteQuestionComponent),
				MockComponent(VerifyElectionGroupComponent),
				MockPipe(SortByPipe, mockSortBy),
				MockPipe(ConcatPipe, mockConcat),
			],
			imports: [
				MockModule(UiComponentsModule),
				TranslateTestingModule.withTranslations({}),
				MockComponent(IconComponent)
			],
			providers: [
				MockStoreProvider(),
				MockProvider(FAQService),
				MockProvider(NgbModal, { open: jest.fn().mockReturnValue({componentInstance:{}}) }),
			],
		}).compileComponents();

		store = TestBed.inject(MockStore);
		modal = TestBed.inject(NgbModal);
		faqService = TestBed.inject(FAQService);
		fixture = TestBed.createComponent(VerifyComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	describe('"abandonned process in previous session" message', () => {
		let sentButNotCastDiv: DebugElement;

		const setFlagSentButNotCast = (flag: boolean) => {
			setState(store, { voteSentButNotCastInPreviousSession: flag });
			fixture.detectChanges();

			sentButNotCastDiv = fixture.debugElement.query(
				By.css('#warningSentButNotCast'),
			);
		};

		it('show "abandonned process in previous session" message if flag is set', () => {
			setFlagSentButNotCast(true);
			expect(sentButNotCastDiv.nativeElement.textContent).toContain(
				'verify.warning.abandonedprocess',
			);
		});

		it('do not show "abandonned process in previous session" message if flag is not set', () => {
			setFlagSentButNotCast(false);
			expect(sentButNotCastDiv).toBeFalsy();
		});
	});

	describe('buttons', () => {
		it('should call showFAQ on click on show-faq-choice-codes-link', () => {
			const faqButton = fixture.debugElement.query(
				By.css(`#show-faq-choice-codes-link`),
			);

			jest.spyOn(faqService, 'showFAQ');
			faqButton.nativeElement.click();

			expect(faqService.showFAQ).toHaveBeenCalledWith(
				FAQSection.WhatAreChoiceReturnCodes,
			);
		});

		it('should open the invalid codes modal on click on the invalid codes button', () => {
			const invalidCodesButton = fixture.debugElement.query(
				By.css(`#invalid-codes`),
			);
			invalidCodesButton.nativeElement.click();

			expect(modal.open).toHaveBeenCalledWith(
				ModalInvalidCodesComponent,
				expect.any(Object),
			);
		});
	});
});
