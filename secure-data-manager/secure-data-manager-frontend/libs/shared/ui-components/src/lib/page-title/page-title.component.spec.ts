/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {PageTitleComponent} from './page-title.component';
import {CommonModule} from '@angular/common';
import {TranslateModule} from '@ngx-translate/core';
import {By} from '@angular/platform-browser';

describe('PageTitleComponent', () => {
	let component: PageTitleComponent;
	let fixture: ComponentFixture<PageTitleComponent>;
	let mainElement: HTMLElement;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [CommonModule, TranslateModule, PageTitleComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(PageTitleComponent);
		component = fixture.componentInstance;

		component.title = 'Test Title';
		component.instructions = 'Test instructions';
		fixture.detectChanges();

		// Simulate the 'main' element that will be listened to for scroll events
		mainElement = document.createElement('main');
		document.body.appendChild(mainElement);
	});

	afterEach(() => {
		document.body.removeChild(mainElement);
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should bind inputs and display title and instructions', () => {
		const titleElement = fixture.debugElement.query(By.css('h1'));
		const instructionsElement = fixture.debugElement.query(By.css('p'));

		expect(titleElement.nativeElement.textContent).toBe(component.title);
		expect(instructionsElement.nativeElement.textContent).toBe(component.instructions);
	});

	it('should apply "sticky" class to the host when the parent scrolls', () => {
		component.ngAfterViewInit();

		mainElement.scrollTop = 100;
		mainElement.dispatchEvent(new Event('scroll'));

		fixture.detectChanges();

		expect(component.isSticky).toBe(true);
		expect(fixture.nativeElement.classList.contains('sticky')).toBe(true);
	});

	it('should not apply "sticky" class to the host when the parent is not scrolled', (done) => {
		component.ngAfterViewInit();

		mainElement.scrollTop = 0;
		mainElement.dispatchEvent(new Event('scroll'));

		fixture.detectChanges();

		expect(component.isSticky).toBe(false);
		expect(fixture.nativeElement.classList.contains('sticky')).toBe(false);
		done();
	});
});
