/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {By} from '@angular/platform-browser';
import {MockStore} from '@ngrx/store/testing';
import {IconComponent} from "@vp/shared-ui-components";
import {ConfirmActions} from '@vp/voter-portal-ui-state';
import {ConfirmComponent} from './confirm.component';
import {MockStoreProvider} from '@vp/shared-util-testing';
import {UiComponentsModule} from '@vp/voter-portal-ui-components';
import {TranslateTestingModule} from 'ngx-translate-testing';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MockComponent, MockModule} from 'ng-mocks';

class MockState {
	voteCastReturnCode = '12345678';
	cast = false;
}

describe('ConfirmComponent', () => {
	let fixture: ComponentFixture<ConfirmComponent>;
	let store: MockStore;
	const initialState = Object.freeze(new MockState());

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [ConfirmComponent],
			imports: [
				TranslateTestingModule.withTranslations({}),
				MockModule(UiComponentsModule),
				MockComponent(IconComponent)
			],
			providers: [
				MockStoreProvider(initialState),
			],
		}).compileComponents();
	});

	beforeEach(() => {
		store = TestBed.inject(MockStore);
		fixture = TestBed.createComponent(ConfirmComponent);
		fixture.detectChanges();
	});

	describe('quit button', () => {
		it('should call quit on click on quit-button', () => {
			jest.spyOn(store, 'dispatch');

			const quitButtonElement = fixture.debugElement.query(
				By.css('#quit-button'),
			).nativeElement;

			quitButtonElement.click();

			expect(store.dispatch).toHaveBeenCalledWith(ConfirmActions.endClicked());
		});
	});
});
