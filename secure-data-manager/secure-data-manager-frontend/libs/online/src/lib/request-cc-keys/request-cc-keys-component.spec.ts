/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {RequestCcKeysComponent} from './request-cc-keys.component';

describe('RequestCcKeysComponent', () => {
	let component: RequestCcKeysComponent;
	let fixture: ComponentFixture<RequestCcKeysComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [RequestCcKeysComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(RequestCcKeysComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
