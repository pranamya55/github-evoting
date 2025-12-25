/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {MockPipe} from "ng-mocks";
import {TranslateTextPipe} from "e-voting-libraries-ui-kit";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentElementEventStateComponent} from './content-element-event-state.component';

describe('ContentElementEventStateComponent', () => {
	let component: ContentElementEventStateComponent;
	let fixture: ComponentFixture<ContentElementEventStateComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [ContentElementEventStateComponent, MockPipe(TranslateTextPipe)],
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementEventStateComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});
});