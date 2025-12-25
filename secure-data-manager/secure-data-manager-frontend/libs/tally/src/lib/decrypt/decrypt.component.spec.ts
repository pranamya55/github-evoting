/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {DecryptComponent} from './decrypt.component';

describe('DecryptionComponent', () => {
	let component: DecryptComponent;
	let fixture: ComponentFixture<DecryptComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [DecryptComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(DecryptComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
