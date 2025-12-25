/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TranslateTestingModule} from 'ngx-translate-testing';

import {PageNotFoundComponent} from './page-not-found.component';
import {UiComponentsModule} from "@vp/voter-portal-ui-components";
import {provideMockStore} from "@ngrx/store/testing";
import {IconComponent} from "@vp/shared-ui-components";

describe('PageNotFoundComponent', () => {
	let component: PageNotFoundComponent;
	let fixture: ComponentFixture<PageNotFoundComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				UiComponentsModule,
				IconComponent,
				TranslateTestingModule.withTranslations({
					FR: {
						'pagenotfound.explanation': 'Message with <strong>bold</strong> text.'
					}
				}).withDefaultLanguage('FR'),
			],
			declarations: [PageNotFoundComponent],
			providers: [provideMockStore({initialState: {}})],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(PageNotFoundComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should show explanation message', () => {
		const strongText: HTMLElement = fixture.nativeElement.querySelector('#explanation > strong');
		expect(strongText).toBeTruthy();
	});
});
