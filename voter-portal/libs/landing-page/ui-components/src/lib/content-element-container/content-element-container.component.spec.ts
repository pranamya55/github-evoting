/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentElementContainerComponent} from './content-element-container.component';

describe('ContentElementContainerComponent', () => {
	let component: ContentElementContainerComponent;
	let fixture: ComponentFixture<ContentElementContainerComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [ContentElementContainerComponent]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementContainerComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('should render the container element', () => {
		fixture.detectChanges();
		const container = fixture.nativeElement.querySelector('div');
		expect(container).toBeTruthy();
		expect(container.classList).toContain('bg-white');
		expect(container.classList).toContain('rounded');
		expect(container.classList).toContain('shadow');
	});
});
