/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {By} from "@angular/platform-browser";
import {TranslatePipe} from "@ngx-translate/core";
import {IconComponent} from "@vp/shared-ui-components";
import {UiComponentsModule} from "@vp/voter-portal-ui-components";
import {MockStoreProvider} from "@vp/shared-util-testing";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {FinalizationPageComponent} from './finalization-page.component';
import {MockPipe} from 'ng-mocks';
import {of} from "rxjs";
import {TranslateTestingModule} from "ngx-translate-testing";

describe('FinalizationPageComponent', () => {
	let component: FinalizationPageComponent;
	let fixture: ComponentFixture<FinalizationPageComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				UiComponentsModule,
				IconComponent,
				TranslateTestingModule.withTranslations({}),
			],
			declarations: [
				FinalizationPageComponent,
				MockPipe(TranslatePipe, (key: string) => key),
			],
			providers: [
				MockStoreProvider(),
			]
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(FinalizationPageComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	describe('vote cast in current session', () => {
		beforeEach(() => {
			component.voteCastInPreviousSession$ = of(false);
			fixture.detectChanges();
		});

		it('should show correct title', () => {
			const title = fixture.debugElement.query(By.css('h1'));
			expect(title.nativeElement.textContent).toContain('finalization.thankyou');
		});

		it('should show correct message', () => {
			const message = fixture.debugElement.query(By.css('.lead'));
			expect(message.nativeElement.textContent).toContain('finalization.votingprocesssuccessful');
		});

		it('should show instruction to clear cache', () => {
			const clearCache = fixture.debugElement.query(By.css('#clearCache'));
			expect(clearCache).toBeTruthy();
		});

		it('should not show finalization code', () => {
			const finalizationCode = fixture.debugElement.query(By.css('vp-finalization-code'));
			expect(finalizationCode).toBeFalsy();
		});
	});

	describe('vote cast in previous session', () => {
		beforeEach(() => {
			component.voteCastInPreviousSession$ = of(true);
			fixture.detectChanges();
		});

		it('should show correct title', () => {
			const title = fixture.debugElement.query(By.css('h1'));
			expect(title.nativeElement.textContent).toContain('finalization.votealreadyregistered');
		});

		it('should show correct message', () => {
			const message = fixture.debugElement.query(By.css('.lead'));
			expect(message.nativeElement.textContent).toContain('finalization.checkcode');
		});

		it('should not show instruction to clear cache', () => {
			const clearCache = fixture.debugElement.query(By.css('#clearCache'));
			expect(clearCache).toBeFalsy();
		});

		it('should not show finalization code', () => {
			const finalizationCode = fixture.debugElement.query(By.css('vp-finalization-code'));
			expect(finalizationCode).toBeTruthy();
		});
	});
});
