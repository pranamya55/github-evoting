/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AccordionComponent } from './accordion.component';
import { MockPipe } from 'ng-mocks';
import { NgbAccordionModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { Component } from '@angular/core';
import { RandomInt, RandomString } from '@vp/shared-util-testing';

@Component({
	standalone: false,
	template: `
		<vp-accordion
			[headingLevel]="headingLevel"
			[containerClass]="containerClass"
			[bodyClass]="bodyClass"
		>
			<ng-container title>{{ title }}</ng-container>
			<ng-container body>{{ body }}</ng-container>
		</vp-accordion>
	`,
})
class TestingHost {
	headingLevel = RandomInt(6, 1);
	containerClass = RandomString();
	bodyClass = RandomString();
	title = RandomString();
	body = RandomString();
}

describe('AccordionComponent', () => {
	let component: TestingHost;
	let fixture: ComponentFixture<TestingHost>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [TestingHost, AccordionComponent, MockPipe(TranslatePipe)],
			imports: [NgbAccordionModule],
		}).compileComponents();

		fixture = TestBed.createComponent(TestingHost);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should render an accordion section', () => {
		const section = fixture.debugElement.query(By.css('section.accordion'));
		expect(section).toBeTruthy();
	});

	it('should render the correct header', () => {
		const accordionHeader: HTMLElement = fixture.debugElement.query(
			By.css('.accordion-header'),
		).nativeElement;
		expect(accordionHeader.localName).toBe(`h${component.headingLevel}`);
	});

	it('should render the title in the accordion button', () => {
		const accordionButton: HTMLElement = fixture.debugElement.query(
			By.css('.accordion-button'),
		).nativeElement;
		expect(accordionButton.textContent).toContain(component.title);
	});

	it('should render the body content in the accordion body', () => {
		const accordionBody: HTMLElement = fixture.debugElement.query(
			By.css('.accordion-body'),
		).nativeElement;
		expect(accordionBody.textContent).toContain(component.body);
	});

	it('should apply provided class to the container', () => {
		const accordionItem = fixture.debugElement.query(By.css('.accordion-item'));
		expect(accordionItem.classes[component.containerClass]).toBeTruthy();
	});

	it('should apply provided class to the body', () => {
		const accordionBody = fixture.debugElement.query(By.css('.accordion-body'));
		expect(accordionBody.classes[component.bodyClass]).toBeTruthy();
	});
});
