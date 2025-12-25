/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {By} from '@angular/platform-browser';
import {Subject} from "rxjs";
import {MockPipe} from 'ng-mocks';
import {IconComponent} from '@vp/shared-ui-components';
import {TranslateTextPipe} from 'e-voting-libraries-ui-kit';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {LangChangeEvent, TranslateService} from "@ngx-translate/core";
import {ContentElementLinkComponent} from "./content-element-link.component";

class MockTranslateService {
	currentLang = 'DE';
	onLangChange = new Subject<LangChangeEvent>();
}

describe('ContentElementLinkComponent', () => {
	let component: ContentElementLinkComponent;
	let fixture: ComponentFixture<ContentElementLinkComponent>;
	let translateService: MockTranslateService;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				ContentElementLinkComponent,
				MockPipe(TranslateTextPipe),
				IconComponent
			],
			providers: [
				{ provide: TranslateService, useClass: MockTranslateService }
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementLinkComponent);
		component = fixture.componentInstance;
		component.element = {
			text: {DE: 'DE', FR: 'FR', IT: 'IT', RM: 'RM'},
			url: ""
		}
		translateService = TestBed.inject(TranslateService) as any;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('renders a link with default values', () => {
		fixture.detectChanges();
		expect(component.primary).toBeFalsy();
		expect(component.secondary).toBeFalsy();
		expect(component.block).toBeFalsy();
		const link: HTMLElement = fixture.nativeElement.querySelector('a');
		expect(link).toBeTruthy();
		expect(link.getAttribute('class')).toContain('link-body-emphasis link-underline-opacity-0 link-underline-opacity-75-hover');
	});

	it('renders primary link when primary input is true', () => {
		component.primary = true;
		fixture.detectChanges();
		expect(component.primary).toBeTruthy();
		const link: HTMLElement = fixture.nativeElement.querySelector('a');
		expect(link).toBeTruthy();
		expect(link.getAttribute('class')).toContain('btn btn-dark');
	});

	it('renders secondary link when secondary input is true', () => {
		component.secondary = true;
		fixture.detectChanges();
		expect(component.secondary).toBeTruthy();
		const link: HTMLElement = fixture.nativeElement.querySelector('a');
		expect(link).toBeTruthy();
		expect(link.getAttribute('class')).toContain('btn btn-outline-dark');
	});

	it('applies block style when block input is true', () => {
		component.block = true;
		fixture.detectChanges();
		expect(component.block).toBeTruthy();
		const link: HTMLElement = fixture.nativeElement.querySelector('a');
		expect(link).toBeTruthy();
		expect(link.getAttribute('class')).toContain('w-100');
	});

	it('applies bold style when element.main is true', () => {
		component.element.main = true;
		fixture.detectChanges();
		expect(component.element.main).toBeTruthy();
		const link: HTMLElement = fixture.nativeElement.querySelector('a');
		expect(link).toBeTruthy();
		expect(link.getAttribute('class')).toContain('fw-bold');
	});

	it('renders icon if element.icon is provided', () => {
		component.element = {
			text: {DE: 'DE', FR: 'FR', IT: 'IT', RM: 'RM'},
			url: "",
			icon: 'arrows'
		}
		fixture.detectChanges();
		const icon = fixture.debugElement.query(By.directive(IconComponent));
		expect(icon).toBeTruthy();
		expect(icon.componentInstance.name).toEqual('arrows');
	});

	it('throws error if element input is missing', () => {
		component.element = null as any; // Simulate missing element
		expect(() => fixture.detectChanges()).toThrow();
	});

	it('current language is updated on language change', () => {
		component.element.addLanguageParameter = true;
		fixture.detectChanges();
		expect(component.currentLanguage).toBe('DE');
		translateService.onLangChange.next({lang: 'IT'} as LangChangeEvent);
		expect(component.currentLanguage).toBe('IT');
	});
});