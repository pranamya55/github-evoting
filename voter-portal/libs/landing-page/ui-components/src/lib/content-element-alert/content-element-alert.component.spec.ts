/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {IconComponent} from '@vp/shared-ui-components';
import {NgTemplateOutlet} from '@angular/common';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentElementAlertComponent} from './content-element-alert.component';

describe('ContentElementAlertComponent', () => {
	let component: ContentElementAlertComponent;
	let fixture: ComponentFixture<ContentElementAlertComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				ContentElementAlertComponent,
				IconComponent,
				NgTemplateOutlet
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementAlertComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('renders the info level', () => {
		component.level = 'info';
		fixture.detectChanges();
		const alert = fixture.nativeElement.querySelector('div.alert');
		expect(alert).toBeTruthy();
		expect(alert.classList).toContain('alert-primary');
	});

	it('renders the warning level', () => {
		component.level = 'warning';
		fixture.detectChanges();
		const alert = fixture.nativeElement.querySelector('div.alert');
		expect(alert).toBeTruthy();
		expect(alert.classList).toContain('alert-warning');
	});

	it('renders the error level', () => {
		component.level = 'error';
		fixture.detectChanges();
		const alert = fixture.nativeElement.querySelector('div.alert');
		expect(alert).toBeTruthy();
		expect(alert.classList).toContain('alert-danger');
	});

	it('renders the success level', () => {
		component.level = 'success';
		fixture.detectChanges();
		const alert = fixture.nativeElement.querySelector('div.alert');
		expect(alert).toBeTruthy();
		expect(alert.classList).toContain('alert-success');
	});
});
