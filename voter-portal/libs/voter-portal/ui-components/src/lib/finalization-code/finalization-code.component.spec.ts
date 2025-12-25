/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {By} from '@angular/platform-browser';
import {IconComponent} from "@vp/shared-ui-components";
import {MockStoreProvider} from "@vp/shared-util-testing";
import {UiComponentsModule} from "@vp/voter-portal-ui-components";
import {TranslateTestingModule} from "ngx-translate-testing";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FinalizationCodeComponent} from './finalization-code.component';
import {MockComponent, MockModule} from "ng-mocks";

class MockState {
	voteCastReturnCode = '12345678';
	cast = false;
}
describe('FinalizationCodeComponent', () => {
	let fixture: ComponentFixture<FinalizationCodeComponent>;
	const initialState = Object.freeze(new MockState());

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [FinalizationCodeComponent],
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
		fixture = TestBed.createComponent(FinalizationCodeComponent);
		fixture.detectChanges();
	});

	describe('finalization code', () => {
		it('should show vote cast return code formatted correctly', () => {
			const voteCastReturnCodeElement = fixture.debugElement.query(
				By.css('#vote-cast-return-code'),
			).nativeElement;
			expect(voteCastReturnCodeElement.textContent).toContain('1234 5678');
		});
	});
});
