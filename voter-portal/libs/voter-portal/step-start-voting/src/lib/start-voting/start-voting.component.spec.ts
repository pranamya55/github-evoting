/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { NgbDatepickerModule } from '@ng-bootstrap/ng-bootstrap';
import { provideMockStore } from '@ngrx/store/testing';
import { MockComponent, MockModule, MockProvider } from 'ng-mocks';
import { NgxMaskDirective, provideNgxMask } from 'ngx-mask';
import { TranslateTestingModule } from 'ngx-translate-testing';
import { ExtendedFactorComponent } from '../extended-factor/extended-factor.component';

import { StartVotingComponent } from './start-voting.component';
import {
	CancelState,
	ConfigurationService,
	ProcessCancellationService,
} from '@vp/voter-portal-ui-services';
import { FAQService } from '@vp/voter-portal-feature-faq';
import { UiComponentsModule } from '@vp/voter-portal-ui-components';
import {
	RandomInt,
	RandomStartVotingKey,
	TranslationListTestingDirective,
} from '@vp/shared-util-testing';
import { ExtendedFactor } from '@vp/voter-portal-util-types';
import {IconComponent} from "@vp/shared-ui-components";

describe('StartVotingComponent', () => {
	let fixture: ComponentFixture<StartVotingComponent>;
	let component: StartVotingComponent;
	let processCancellationService: ProcessCancellationService;
	let faqService: FAQService;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				ReactiveFormsModule,
				MockModule(UiComponentsModule),
				MockComponent(IconComponent),
				MockModule(NgbDatepickerModule),
				HttpClientTestingModule,
				RouterTestingModule,
				TranslateTestingModule.withTranslations({}),
				NgxMaskDirective,
			],
			providers: [
				provideMockStore({}),
				MockProvider(ProcessCancellationService, {
					cancelState: CancelState.NO_CANCEL_VOTE_OR_LEAVE_PROCESS,
				}),
				MockProvider(FAQService),
				MockProvider(ConfigurationService),
				provideNgxMask(),
			],
			declarations: [
				StartVotingComponent,
				TranslationListTestingDirective,
				MockComponent(ExtendedFactorComponent),
			],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(StartVotingComponent);
		component = fixture.componentInstance;

		processCancellationService = TestBed.inject(ProcessCancellationService);
		faqService = TestBed.inject(FAQService);

		fixture.detectChanges();
	});

	it('should properly show a title', () => {
		const title = fixture.debugElement.query(By.css('h1')).nativeElement;
		expect(title?.textContent).toContain('startvoting.title');
	});

	it('should open the FAQ when clicking the corresponding button', () => {
		const faqButton = fixture.debugElement.query(
			By.css(`#show-faq-link`),
		).nativeElement;
		jest.spyOn(faqService, 'showFAQ');

		faqButton.click();

		expect(faqService.showFAQ).toHaveBeenCalled();
	});

	describe('start voting key input', () => {
		function getValidationError(): DebugElement {
			return fixture.debugElement.query(By.css('#startVotingKey-required'));
		}

		function submitForm(): void {
			const submitButton = fixture.debugElement.query(
				By.css('#btn_start_voting'),
			);
			submitButton.nativeElement.click();
		}

		it('should not show any validation error by default', () => {
			expect(getValidationError()).toBeFalsy();
		});

		describe('correct value', () => {
			beforeEach(() => {
				component.startVotingKey.setValue(RandomStartVotingKey());
				fixture.detectChanges();
			});

			it('should mark the initialisation code form control as valid', () => {
				expect(component.startVotingKey.valid).toBeTruthy();
			});

			it('should not show a validation error on submit', () => {
				submitForm();
				fixture.detectChanges();
				expect(getValidationError()).toBeFalsy();
			});
		});

		describe('incorrect value', () => {
			beforeEach(() => {
				const validCode = RandomStartVotingKey();
				const invalidCode = validCode.substring(
					0,
					RandomInt(validCode.length - 1),
				);
				component.startVotingKey.setValue(invalidCode);
				fixture.detectChanges();
			});

			it('should mark the initialisation code form control as invalid', () => {
				expect(component.startVotingKey.valid).toBeFalsy();
			});

			it('should show a validation error on submit', () => {
				submitForm();
				fixture.detectChanges();
				expect(getValidationError()).toBeTruthy();
			});
		});
	});

	describe('information messages', () => {
		function getInfoMessage(id: string): DebugElement {
			return fixture.debugElement.query(By.css(`#${id}`));
		}

		it('should not show any information message by default', () => {
			['cancelMessage', 'leaveMessage', 'quitMessage'].forEach((messageId) => {
				expect(getInfoMessage(messageId)).toBeFalsy();
			});
		});

		it('should show the proper message if the voting process was cancelled', () => {
			processCancellationService.cancelState = CancelState.CANCEL_VOTE;

			fixture.detectChanges();

			const cancelMessage = getInfoMessage('cancelMessage');
			expect(cancelMessage).toBeTruthy();
			expect(cancelMessage.nativeElement.textContent).toContain(
				'startvoting.cancel',
			);
		});

		it('should show the proper message if the voting process was left', () => {
			processCancellationService.cancelState = CancelState.LEAVE_PROCESS;

			Object.values(ExtendedFactor).forEach((extendedFactor) => {
				component.configuration.identification = extendedFactor;
				fixture.detectChanges();

				const leaveMessage = getInfoMessage('leaveMessage');
				expect(leaveMessage).toBeTruthy();
				expect(leaveMessage.nativeElement.textContent).toContain(
					`startvoting.leave.${extendedFactor}`,
				);
			});
		});
	});
});
