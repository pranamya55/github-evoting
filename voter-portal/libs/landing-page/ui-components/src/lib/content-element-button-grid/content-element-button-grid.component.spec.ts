/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {MockComponent} from 'ng-mocks';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentElementButtonComponent} from '../content-element-button/content-element-button.component';
import {ContentElementButtonGridComponent} from './content-element-button-grid.component';

describe('ContentElementButtonGridComponent', () => {
	let component: ContentElementButtonGridComponent;
	let fixture: ComponentFixture<ContentElementButtonGridComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				ContentElementButtonGridComponent,
				MockComponent(ContentElementButtonComponent)
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementButtonGridComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('renders all buttons', () => {
		component.element = {
			primary: {text: {DE: 'DE1', FR: 'FR1', IT: 'IT1', RM: 'RM1'}, link: ''},
			secondary: {text: {DE: 'DE2', FR: 'FR2', IT: 'IT2', RM: 'RM2'}, link: ''}
		} as any;
		fixture.detectChanges();
		const buttons = fixture.nativeElement.querySelectorAll('vp-content-element-button');
		expect(buttons.length).toBe(2);
	});

	it('renders only primary button if no secondary button', () => {
		component.element = {
			primary: {text: {DE: 'DE1', FR: 'FR1', IT: 'IT1', RM: 'RM1'}, link: ''}
		} as any;
		fixture.detectChanges();
		const buttons = fixture.nativeElement.querySelectorAll('vp-content-element-button');
		expect(buttons.length).toBe(1);
	});

	it('renders only secondary button if no primary button', () => {
		component.element = {
			secondary: {text: {DE: 'DE2', FR: 'FR2', IT: 'IT2', RM: 'RM2'}, link: ''}
		} as any;
		fixture.detectChanges();
		const buttons = fixture.nativeElement.querySelectorAll('vp-content-element-button');
		expect(buttons.length).toBe(1);
	});

	it('throws error if element input is missing', () => {
		component.element = null as any;
		expect(() => fixture.detectChanges()).toThrow();
	});
});