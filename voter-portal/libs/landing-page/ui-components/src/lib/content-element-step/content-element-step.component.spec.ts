/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {MockPipe} from "ng-mocks";
import {TranslateTextPipe} from "e-voting-libraries-ui-kit";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentElementStepComponent} from './content-element-step.component';

describe('EventElementStepComponent', () => {
	let component: ContentElementStepComponent;
	let fixture: ComponentFixture<ContentElementStepComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [ContentElementStepComponent, MockPipe(TranslateTextPipe)],
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementStepComponent);
		component = fixture.componentInstance;
	});

	it('should create the component when index and element are provided', () => {
		component.index = 0;
		component.element = {} as any;
		fixture.detectChanges();
		expect(component).toBeTruthy();
	});

	it('should throw if element input is missing', () => {
		component.index = 0;
		expect(() => fixture.detectChanges()).toThrow();
	});
});