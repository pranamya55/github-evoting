/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {of, Subject} from "rxjs";
import {EventElement, TenantStateEvent} from "@vp/landing-page-utils-types";
import {ActivatedRoute} from "@angular/router";
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TenantConfigurationService} from "@vp/landing-page-data-access";
import {ContentElementEventReferenceComponent} from './content-element-event-reference.component';

describe('ContentElementEventReferenceComponent', () => {
	let component: ContentElementEventReferenceComponent;
	let fixture: ComponentFixture<ContentElementEventReferenceComponent>;

	const mockQueryParams$ = new Subject<any>();
	const mockActivatedRoute = {
		queryParamMap: mockQueryParams$.asObservable()
	};

	const mockEvents: EventElement[] = [];
	const mockStates: TenantStateEvent[] = [];
	const mockTenantConfigurationService = {
		getEvents: jest.fn(() => of(mockEvents)),
		getStates: jest.fn(() => of(mockStates))
	};

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [ContentElementEventReferenceComponent],
			providers: [
				{provide: TenantConfigurationService, useValue: mockTenantConfigurationService},
				{provide: ActivatedRoute, useValue: mockActivatedRoute}
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementEventReferenceComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('should set contentElements based on phase override in query params', () => {
		const mockEvent = {
			id: 'event1',
			phases: [
				{ phaseLevel: 'OPEN', contentElements: ['elementA'] },
				{ phaseLevel: 'CLOSED', contentElements: ['elementB'] }
			]
		};
		const mockState = [{ id: 'event1', activePhase: 'CLOSED' }];
		mockEvents.push(mockEvent as any);
		mockTenantConfigurationService.getStates = jest.fn(() => of(mockState));
		component.element = { id: 'event1', contentElements: [] } as any;

		fixture.detectChanges();
		mockQueryParams$.next({
			get: (key: string) => key === 'phases' ? 'event1,OPEN' : null
		});

		expect(component.element.contentElements).toEqual(['elementA']);
	});

	it('should set contentElements based on active phase if no phase override', () => {
		const mockEvent = {
			id: 'event2',
			phases: [
				{ phaseLevel: 'UPCOMING', contentElements: ['elementC'] },
				{ phaseLevel: 'EVOTING-CLOSED', contentElements: ['elementD'] }
			]
		};
		const mockState = [{ id: 'event2', activePhase: 'EVOTING-CLOSED' }];
		mockEvents.push(mockEvent as any);
		mockTenantConfigurationService.getStates = jest.fn(() => of(mockState));
		component.element = { id: 'event2', contentElements: [] } as any;

		fixture.detectChanges();
		mockQueryParams$.next({
			get: (_: string) => null
		});

		expect(component.element.contentElements).toEqual(['elementD']);
	});

	it('should set contentElements to empty array if event not found', () => {
		mockTenantConfigurationService.getStates = jest.fn(() => of([]));
		component.element = { id: 'unknown', contentElements: ['shouldBeCleared'] } as any;

		fixture.detectChanges();
		mockQueryParams$.next({
			get: (_: string) => null
		});

		expect(component.element.contentElements).toEqual([]);
	});

	it('should set contentElements to empty array if phase override does not match any phase', () => {
		const mockEvent = {
			id: 'event3',
			phases: [
				{ phaseLevel: 'UPCOMING', contentElements: ['elementE'] }
			]
		};
		const mockState = [{ id: 'event3', activePhase: 'UPCOMING' }];
		mockEvents.push(mockEvent as any);
		mockTenantConfigurationService.getStates = jest.fn(() => of(mockState));
		component.element = { id: 'event3', contentElements: [] } as any;

		fixture.detectChanges();
		mockQueryParams$.next({
			get: (_: string) => 'event3,NONEXISTENT'
		});

		expect(component.element.contentElements).toEqual(['elementE']);
	});

	it('should set contentElements to empty array if no active phase found', () => {
		const mockEvent = {
			id: 'event4',
			phases: [
				{ phaseLevel: 'UPCOMING', contentElements: ['elementF'] }
			]
		};
		mockEvents.push(mockEvent as any);
		mockTenantConfigurationService.getStates = jest.fn(() => of([{ id: 'event4', activePhase: 'NOTFOUND' }]));
		component.element = { id: 'event4', contentElements: [] } as any;

		fixture.detectChanges();
		mockQueryParams$.next({
			get: (_: string) => null
		});

		expect(component.element.contentElements).toEqual([]);
	});

});