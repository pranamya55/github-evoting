/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {MixAndDownloadComponent} from './mix-and-download.component';

describe('MixAndDownloadComponent', () => {
	let component: MixAndDownloadComponent;
	let fixture: ComponentFixture<MixAndDownloadComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [MixAndDownloadComponent],
		}).compileComponents();

		fixture = TestBed.createComponent(MixAndDownloadComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create', () => {
		expect(component).toBeTruthy();
	});
});
