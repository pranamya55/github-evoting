/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentElementAccordionComponent} from './content-element-accordion.component';
import {MockComponent, MockPipe} from 'ng-mocks';
import {AccordionComponent, IconComponent} from '@vp/shared-ui-components';
import {TranslateTextPipe} from 'e-voting-libraries-ui-kit';

describe('ContentElementAccordionComponent', () => {
	let component: ContentElementAccordionComponent;
	let fixture: ComponentFixture<ContentElementAccordionComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				ContentElementAccordionComponent,
				MockComponent(AccordionComponent),
				MockComponent(IconComponent),
				MockPipe(TranslateTextPipe)
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementAccordionComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('creates the component when element is provided', () => {
		component.element = { level: 1, title: 'Title', icon: 'arrow-right', contentElements: [] } as any;
		fixture.detectChanges();
		expect(component).toBeTruthy();
	});

	it('throws an error if element is undefined', () => {
		// @ts-expect-error
		component.element = undefined;
		expect(() => fixture.detectChanges()).toThrow();
	});

});