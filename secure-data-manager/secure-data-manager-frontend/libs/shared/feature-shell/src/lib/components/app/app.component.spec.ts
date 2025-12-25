/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ComponentFixture, TestBed} from '@angular/core/testing';
import {RouterOutlet} from '@angular/router';
import {MockComponent, MockDirective} from 'ng-mocks';
import {HeaderComponent} from '../header/header.component';
import {AppComponent} from './app.component';
import {By} from '@angular/platform-browser';
import {StepperComponent} from '../stepper/stepper.component';

describe('AppComponent', () => {
	let fixture: ComponentFixture<AppComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				AppComponent,
				MockComponent(HeaderComponent),
				MockComponent(StepperComponent),
				MockDirective(RouterOutlet),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(AppComponent);
		fixture.detectChanges();
	});

	it('should show a header', () => {
		const header = fixture.debugElement.query(By.css('sdm-header'));
		expect(header).toBeTruthy();
	});

	it('should show a stepper', () => {
		const stepper = fixture.debugElement.query(By.css('sdm-stepper'));
		expect(stepper).toBeTruthy();
	});

	it('should show a router outlet', () => {
		const routerOutlet = fixture.debugElement.query(By.css('router-outlet'));
		expect(routerOutlet).toBeTruthy();
	});
});
