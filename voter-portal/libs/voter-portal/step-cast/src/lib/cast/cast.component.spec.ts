/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CastComponent } from './cast.component';
import {MockComponent, MockDirective, MockModule, MockPipe, MockProvider} from 'ng-mocks';
import { UiComponentsModule } from '@vp/voter-portal-ui-components';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { FAQService } from '@vp/voter-portal-feature-faq';
import { ReactiveFormsModule } from '@angular/forms';
import {
	MockStoreProvider,
	setState,
	TranslationListTestingDirective,
} from '@vp/shared-util-testing';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { MockStore } from '@ngrx/store/testing';
import { VerifyActions } from '@vp/voter-portal-ui-state';
import { RouterTestingModule } from '@angular/router/testing';
import {IconComponent} from "@vp/shared-ui-components";
import {NgxMaskDirective, provideNgxMask} from "ngx-mask";

describe('CastComponent', () => {
	let component: CastComponent;
	let fixture: ComponentFixture<CastComponent>;
	let store: MockStore;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				CastComponent,
				MockPipe(TranslatePipe),
				MockDirective(TranslationListTestingDirective),
			],
			imports: [
				ReactiveFormsModule,
				RouterTestingModule,
				MockModule(UiComponentsModule),
				MockComponent(IconComponent),
				NgxMaskDirective,
			],
			providers: [
				MockStoreProvider({ loading: false }),
				MockProvider(FAQService),
				MockProvider(TranslateService),
				provideNgxMask(),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(CastComponent);
		component = fixture.componentInstance;
		store = TestBed.inject(MockStore);
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	describe('form validation', () => {
		beforeEach(() => {
			fixture.detectChanges();
		});

		describe('initialState', () => {
			it('should not show validation error without clicking submit button', () => {
				const validationError = fixture.debugElement.query(
					By.css('#confirmation-key-required'),
				);
				expect(validationError).toBeFalsy();
			});
		});

		describe('validators', () => {
			const setInputValue = (value: string) => {
				const confirmationKeyInputElement = fixture.debugElement.query(
					By.css('#confirmationKey'),
				).nativeElement;
				confirmationKeyInputElement.value = value;
				confirmationKeyInputElement.dispatchEvent(new Event('input'));

				fixture.detectChanges();
			};

			it('should not validate with partial input', () => {
				setInputValue('1234');
				expect(component.confirmationKey.invalid).toBeTruthy();
			});

			it('should validate with valid input', () => {
				setInputValue('123456789');
				expect(component.confirmationKey.invalid).toBeFalsy();
			});
		});

		describe('validation error messages', () => {
			let validationErrorDiv: DebugElement;

			const setConfirmationKeyAndClickSubmit = (confirmationKey: string) => {
				component.confirmationKey.setValue(confirmationKey);
				fixture.detectChanges();

				const confirmButtonElement = fixture.debugElement.query(
					By.css('#btn_confirm_vote'),
				).nativeElement;

				confirmButtonElement.click();
				fixture.detectChanges();

				validationErrorDiv = fixture.debugElement.query(
					By.css('#confirmation-key-required'),
				);
			};

			it('should show validation error without input after submit button is clicked', () => {
				setConfirmationKeyAndClickSubmit('');
				expect(validationErrorDiv).toBeTruthy();
			});

			it('should not show validation error with correct input after submit button is clicked', () => {
				setConfirmationKeyAndClickSubmit('123456789');
				expect(validationErrorDiv).toBeFalsy();
			});
		});

		describe('check call to dispatch store action', () => {
			const castVoteWithKey = (value: string) => {
				component.confirmationKey.setValue(value);
				fixture.detectChanges();
				component.cast();
			};

			beforeEach(() => {
				jest.spyOn(store, 'dispatch');
			});

			it('should call dispatch action if input was valid', () => {
				const mockConfirmationKey = '123456789';
				castVoteWithKey(mockConfirmationKey);
				expect(store.dispatch).toHaveBeenCalledWith(
					VerifyActions.castVoteClicked({
						confirmationKey: mockConfirmationKey,
					}),
				);
			});

			it('should not call dispatch action if input was not valid', () => {
				castVoteWithKey('');
				expect(store.dispatch).not.toHaveBeenCalled();
			});
		});
	});

	describe('disable submit button while loading', () => {
		let confirmButton: DebugElement;
		const setLoadingState = (isLoading: boolean) => {
			setState(store, { loading: isLoading });
			fixture.detectChanges();

			confirmButton = fixture.debugElement.query(By.css('#btn_confirm_vote'));
		};

		it('should not disable button while not loading data', () => {
			setLoadingState(false);
			expect(confirmButton.nativeElement.disabled).toBeFalsy();
		});

		it('should disable button while loading data', () => {
			setLoadingState(true);
			expect(confirmButton.nativeElement.disabled).toBeTruthy();
		});
	});
});
