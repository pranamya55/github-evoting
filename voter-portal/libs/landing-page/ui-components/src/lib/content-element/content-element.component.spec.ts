/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {MockComponent} from "ng-mocks";
import {ContentElement} from '@vp/landing-page-utils-types';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {ContentElementComponent} from './content-element.component';
import {ContentElementAlertComponent} from "../content-element-alert/content-element-alert.component";
import {ContentElementButtonComponent} from "../content-element-button/content-element-button.component";
import {ContentElementHeadingComponent} from "../content-element-heading/content-element-heading.component";
import {ContentElementAccordionComponent} from "../content-element-accordion/content-element-accordion.component";
import {ContentElementContainerComponent} from "../content-element-container/content-element-container.component";
import {ContentElementParagraphComponent} from "../content-element-paragraph/content-element-paragraph.component";
import {ContentElementButtonGridComponent} from "../content-element-button-grid/content-element-button-grid.component";

describe('ContentElementComponent', () => {
	let component: ContentElementComponent;
	let fixture: ComponentFixture<ContentElementComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [ContentElementComponent,
				MockComponent(ContentElementAlertComponent),
				MockComponent(ContentElementAccordionComponent),
				MockComponent(ContentElementButtonComponent),
				MockComponent(ContentElementButtonGridComponent),
				MockComponent(ContentElementContainerComponent),
				MockComponent(ContentElementHeadingComponent),
				MockComponent(ContentElementParagraphComponent),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(ContentElementComponent);
		component = fixture.componentInstance;
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('should render heading content element', () => {
		component.contentElement = {
			heading: {
				level: 1,
				text: {DE: 'DE', FR: 'FR', IT: 'IT', RM: 'RM'}
			}
		} as unknown as ContentElement;
		fixture.detectChanges();
		expect(component.isHeadingContentElement(component.contentElement)).toBeTruthy();
	});

	it('should render accordion content element', () => {
		component.contentElement = {
			accordion: {
				level: 1,
				title: {DE: 'DE', FR: 'FR', IT: 'IT', RM: 'RM'},
				icon: 'arrow-right',
				contentElements: []
			}
		} as unknown as ContentElement;
		fixture.detectChanges();
		expect(component.isAccordionContentElement(component.contentElement)).toBeTruthy();
	});

	it('should render button content element', () => {
		component.contentElement = {
			button: {
				link: "",
				text: {DE: 'DE', FR: 'FR', IT: 'IT', RM: 'RM'}
			}
		} as unknown as ContentElement;
		fixture.detectChanges();
		expect(component.isButtonContentElement(component.contentElement)).toBeTruthy();
	});

	it('should render paragraph content element', () => {
		component.contentElement = {
			paragraph: {
				text: {DE: 'DE', FR: 'FR', IT: 'IT', RM: 'RM'}
			}
		} as unknown as ContentElement;
		fixture.detectChanges();
		expect(component.isParagraphContentElement(component.contentElement)).toBeTruthy();
	});

	it('should render alert content element', () => {
		component.contentElement = {
			alert: {
				level: "info",
				contentElements: []
			}
		} as unknown as ContentElement;
		fixture.detectChanges();
		expect(component.isAlertContentElement(component.contentElement)).toBeTruthy();
	});

	it('should render container content element', () => {
		component.contentElement = {
			container: {
				contentElements: []
			}
		} as unknown as ContentElement;
		fixture.detectChanges();
		expect(component.isContainerContentElement(component.contentElement)).toBeTruthy();
	});

	it('should render button grid content element', () => {
		component.contentElement = {
			buttonGrid: {
				primary: {
					text: {DE: 'DE', FR: 'FR', IT: 'IT', RM: 'RM'},
					link: ""
				}
			}
		} as unknown as ContentElement;
		fixture.detectChanges();
		expect(component.isButtonGridContentElement(component.contentElement)).toBeTruthy();
	});

	it('should render button content element', () => {
		component.contentElement = {
			button: {
				text: {DE: 'DE', FR: 'FR', IT: 'IT', RM: 'RM'},
				link: ""
			}
		} as unknown as ContentElement;
		fixture.detectChanges();
		expect(component.isButtonContentElement(component.contentElement)).toBeTruthy();
	});

	it('should not match any type for unknown content element', () => {
		component.contentElement = {type: 'unknown'} as unknown as ContentElement;
		fixture.detectChanges();
		expect(component.isHeadingContentElement(component.contentElement)).toBeFalsy();
		expect(component.isParagraphContentElement(component.contentElement)).toBeFalsy();
		expect(component.isAlertContentElement(component.contentElement)).toBeFalsy();
		expect(component.isContainerContentElement(component.contentElement)).toBeFalsy();
		expect(component.isButtonGridContentElement(component.contentElement)).toBeFalsy();
		expect(component.isButtonContentElement(component.contentElement)).toBeFalsy();
	});
});