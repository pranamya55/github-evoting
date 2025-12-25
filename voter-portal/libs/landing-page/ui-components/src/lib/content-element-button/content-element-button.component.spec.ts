/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentElementButtonComponent} from './content-element-button.component';
import {By} from "@angular/platform-browser";
import {MockComponent} from "ng-mocks";
import {ContentElementLinkComponent} from "@vp/landing-page-ui-components";
import {DebugElement} from "@angular/core";

describe('ContentElementButtonComponent', () => {
	let component: ContentElementButtonComponent;
	let fixture: ComponentFixture<ContentElementButtonComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				ContentElementButtonComponent,
				MockComponent(ContentElementLinkComponent),
			]
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementButtonComponent);
		component = fixture.componentInstance;
		component.element = {
			text: {DE: 'DE', FR: 'FR', IT: 'IT', RM: 'RM'},
			url: ""
		}
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('renders primary button with default values', () => {
		fixture.detectChanges();
		expect(component.secondary).toBeFalsy();
		expect(component.grid).toBeFalsy();
		const link = fixture.debugElement.query(By.directive(ContentElementLinkComponent));
		expect(link).toBeTruthy();
		expect(link.componentInstance.primary).toBeTruthy();
		expect(link.componentInstance.secondary).toBeFalsy();
	});

	it('renders secondary button when secondary input is true', () => {
		component.secondary = true;
		fixture.detectChanges();
		expect(component.secondary).toBeTruthy();
		const link = fixture.debugElement.query(By.directive(ContentElementLinkComponent));
		expect(link).toBeTruthy();
		expect(link.componentInstance.primary).toBeFalsy();
		expect(link.componentInstance.secondary).toBeTruthy();
	});

	it('renders block button when grid input is true', () => {
		component.grid = true;
		fixture.detectChanges();
		expect(component.grid).toBeTruthy();
		const link = fixture.debugElement.query(By.directive(ContentElementLinkComponent));
		expect(link).toBeTruthy();
		expect(link.componentInstance.block).toBeTruthy();
	});
});