/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {DynamicHeadingComponent} from '@vp/shared-ui-components';
import {MockComponent, MockPipe} from 'ng-mocks';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentElementHeadingComponent} from './content-element-heading.component';
import {MarkdownPipe, TranslateTextPipe} from 'e-voting-libraries-ui-kit';

describe('ContentElementHeadingComponent', () => {
	let component: ContentElementHeadingComponent;
	let fixture: ComponentFixture<ContentElementHeadingComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				ContentElementHeadingComponent,
				MockComponent(DynamicHeadingComponent),
				MockPipe(MarkdownPipe),
				MockPipe(TranslateTextPipe)
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementHeadingComponent);
		component = fixture.componentInstance;
	});

	it('creates the component when element is provided', () => {
		component.element = { heading: 'Title', level: 2 } as any;
		fixture.detectChanges();
		expect(component).toBeTruthy();
	});

	it('throws an error if element is undefined', () => {
		// @ts-expect-error
		component.element = undefined;
		expect(() => fixture.detectChanges()).toThrow();
	});

	it('handles a heading with a specific level', () => {
		component.element = { heading: 'Level 3 Title', level: 3 } as any;
		fixture.detectChanges();
		expect(component.element.level).toBe(3);
	});

	it('handles the minimum heading level', () => {
		component.element = { heading: 'Title', level: 1 } as any;
		fixture.detectChanges();
		expect(component.element.level).toBe(1);
	});

	it('handles the maximum heading level', () => {
		component.element = { heading: 'Title', level: 6 } as any;
		fixture.detectChanges();
		expect(component.element.level).toBe(6);
	});
});