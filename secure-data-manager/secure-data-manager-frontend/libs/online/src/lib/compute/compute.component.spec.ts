/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TranslateModule} from '@ngx-translate/core';
import {NgbProgressbar} from '@ng-bootstrap/ng-bootstrap';
import {MockComponent, MockDirective, MockModule, MockProvider,} from 'ng-mocks';
import {ToastService} from '@sdm/shared-ui-services';
import {RouterLink} from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {ComputeComponent} from './compute.component';

describe('ComputationComponent', () => {
	let component: ComputeComponent;
	let fixture: ComponentFixture<ComputeComponent>;

	beforeEach(async () => {
		jest.useFakeTimers();

		await TestBed.configureTestingModule({
			imports: [
				ComputeComponent,
				MockModule(TranslateModule),
				MockDirective(RouterLink),
				MockComponent(NgbProgressbar),
			],
			providers: [
				MockProvider(HttpClient),
				MockProvider(ToastService),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(ComputeComponent);
		component = fixture.componentInstance;

		fixture.detectChanges();
	});

	it('should be created', () => {
		expect(component).toBeTruthy();
	});
});
