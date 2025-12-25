/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {of, Subject} from "rxjs";
import {EventElement} from "@vp/landing-page-utils-types";
import {ActivatedRoute} from "@angular/router";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TenantConfigurationService} from "@vp/landing-page-data-access";
import {ContentElementSharedReferenceComponent} from './content-element-shared-reference.component';

describe('EventElementStepComponent', () => {
	let component: ContentElementSharedReferenceComponent;
	let fixture: ComponentFixture<ContentElementSharedReferenceComponent>;

	const mockQueryParams$ = new Subject<any>();
	const mockActivatedRoute = {
		queryParamMap: mockQueryParams$.asObservable()
	};

	const mockEvents: EventElement[] = [];
	const mockTenantConfigurationService = {
		getEvents: jest.fn(() => of(mockEvents))
	};

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [ContentElementSharedReferenceComponent],
			providers: [
				{provide: TenantConfigurationService, useValue: mockTenantConfigurationService},
				{provide: ActivatedRoute, useValue: mockActivatedRoute}
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementSharedReferenceComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});
});