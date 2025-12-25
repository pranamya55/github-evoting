/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';
import {ContentElementStepComponent} from "../content-element-step/content-element-step.component";
import {ContentElementAlertComponent} from "../content-element-alert/content-element-alert.component";
import {ContentElementButtonComponent} from "../content-element-button/content-element-button.component";
import {ContentElementCaptchaComponent} from "../content-element-captcha/content-element-captcha.component";
import {ContentElementHeadingComponent} from "../content-element-heading/content-element-heading.component";
import {ContentElementAccordionComponent} from "../content-element-accordion/content-element-accordion.component";
import {ContentElementContainerComponent} from "../content-element-container/content-element-container.component";
import {ContentElementParagraphComponent} from "../content-element-paragraph/content-element-paragraph.component";
import {ContentElementButtonGridComponent} from "../content-element-button-grid/content-element-button-grid.component";
import {
	ContentElement,
	isAccordionContentElement,
	isAlertContentElement,
	isButtonContentElement,
	isButtonGridContentElement,
	isCaptchaContentElement,
	isContainerContentElement,
	isEventReferenceContentElement, isEventStateContentElement, isHashesContentElement,
	isHeadingContentElement,
	isParagraphContentElement, isSharedReferenceContentElement,
	isStepsContentElement
} from "@vp/landing-page-utils-types";
import {ContentElementEventReferenceComponent} from "../content-element-event-reference/content-element-event-reference.component";
import {ContentElementEventStateComponent} from "../content-element-event-state/content-element-event-state.component";
import {ContentElementHashesComponent} from "../content-element-hashes/content-element-hashes.component";
import {ContentElementSharedReferenceComponent} from "../content-element-shared-reference/content-element-shared-reference.component";


@Component({
	selector: 'vp-content-element',
	standalone: true,
	templateUrl: './content-element.component.html',
	imports: [
		ContentElementAccordionComponent,
		ContentElementAlertComponent,
		ContentElementButtonComponent,
		ContentElementButtonGridComponent,
		ContentElementCaptchaComponent,
		ContentElementContainerComponent,
		ContentElementEventReferenceComponent,
		ContentElementEventStateComponent,
		ContentElementHashesComponent,
		ContentElementHeadingComponent,
		ContentElementParagraphComponent,
		ContentElementSharedReferenceComponent,
		ContentElementStepComponent
	]
})
export class ContentElementComponent {
	@Input({ required: true }) contentElement!: ContentElement;
	readonly isAccordionContentElement = isAccordionContentElement;
	readonly isAlertContentElement = isAlertContentElement;
	readonly isButtonContentElement = isButtonContentElement;
	readonly isButtonGridContentElement = isButtonGridContentElement;
	readonly isCaptchaContentElement = isCaptchaContentElement;
	readonly isContainerContentElement = isContainerContentElement;
	readonly isEventReferenceContentElement = isEventReferenceContentElement;
	readonly isEventStateContentElement = isEventStateContentElement;
	readonly isHashesContentElement = isHashesContentElement;
	readonly isHeadingContentElement = isHeadingContentElement;
	readonly isParagraphContentElement = isParagraphContentElement;
	readonly isSharedReferenceContentElement = isSharedReferenceContentElement;
	readonly isStepsContentElement = isStepsContentElement;
}