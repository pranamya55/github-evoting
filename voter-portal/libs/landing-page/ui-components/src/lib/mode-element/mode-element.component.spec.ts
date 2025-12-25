/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {of, Subject} from "rxjs";
import {ModeElement} from "@vp/landing-page-utils-types";
import {ActivatedRoute} from "@angular/router";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TenantConfigurationService} from "@vp/landing-page-data-access";
import {ModeElementComponent} from './mode-element.component';

describe('ModeElementComponent', () => {
	let component: ModeElementComponent;
	let fixture: ComponentFixture<ModeElementComponent>;

	const mockQueryParams$ = new Subject<any>();
	const mockActivatedRoute = {
		queryParamMap: mockQueryParams$.asObservable()
	};

	const mockMode: ModeElement = {
		id: "mode-id",
		hideLandingPageContent: true,
		contentElements: []
	};
	const mockTenantConfigurationService = {
		getMode: jest.fn(() => of(mockMode))
	};

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [ModeElementComponent],
			providers: [
				{provide: TenantConfigurationService, useValue: mockTenantConfigurationService},
				{provide: ActivatedRoute, useValue: mockActivatedRoute}
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ModeElementComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});
});