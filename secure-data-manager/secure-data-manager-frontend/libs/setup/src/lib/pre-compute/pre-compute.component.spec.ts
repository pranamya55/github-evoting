/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {HttpClient} from '@angular/common/http';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {NgbProgressbar} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule} from '@ngx-translate/core';
import {ToastService} from '@sdm/shared-ui-services';
import {ProgressComponent} from '@sdm/shared-feature-progress';
import {MockComponent, MockModule, MockProvider,} from 'ng-mocks';
import {PreComputeComponent} from './pre-compute.component';
import {NextButtonComponent} from "@sdm/shared-ui-components";

describe('PreComputationComponent', () => {
	let component: PreComputeComponent;
	let fixture: ComponentFixture<PreComputeComponent>;

	beforeEach(async () => {
		jest.useFakeTimers();

		await TestBed.configureTestingModule({
			imports: [
				PreComputeComponent,
				MockModule(TranslateModule),
				MockComponent(NextButtonComponent),
				MockComponent(NgbProgressbar),
				MockComponent(ProgressComponent),
			],
			providers: [MockProvider(HttpClient), MockProvider(ToastService)],
		}).compileComponents();

		fixture = TestBed.createComponent(PreComputeComponent);
		component = fixture.componentInstance;

		fixture.detectChanges();
	});

	it('should be created', () => {
		expect(component).toBeTruthy();
	});
});
