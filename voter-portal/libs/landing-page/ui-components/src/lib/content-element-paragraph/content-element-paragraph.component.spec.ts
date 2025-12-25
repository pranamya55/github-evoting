/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {MockPipe} from 'ng-mocks';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TranslateTextPipe, MarkdownPipe} from 'e-voting-libraries-ui-kit';
import {ContentElementParagraphComponent} from './content-element-paragraph.component';

describe('ContentElementParagraphComponent', () => {
	let component: ContentElementParagraphComponent;
	let fixture: ComponentFixture<ContentElementParagraphComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				ContentElementParagraphComponent,
				MockPipe(TranslateTextPipe),
				MockPipe(MarkdownPipe)
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementParagraphComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('renders paragraph when element input is provided', () => {
		component.element = {text: {DE: '', FR: '', IT: '', RM: ''}} as any;
		fixture.detectChanges();
		const paragraph = fixture.nativeElement.querySelector('p');
		expect(paragraph).toBeTruthy();
	});

	it('throws error if element input is missing', () => {
		expect(() => fixture.detectChanges()).toThrow();
	});
});