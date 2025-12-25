/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */


import {Component, Input} from "@angular/core";
import {TranslateTextPipe} from "e-voting-libraries-ui-kit";
import {StepContentElement} from "@vp/landing-page-utils-types";
import {DynamicHeadingComponent} from "@vp/shared-ui-components";
import {ContentElementContainerComponent} from "../content-element-container/content-element-container.component";

@Component({
	selector: 'vp-content-element-step',
	standalone: true,
	imports: [
		TranslateTextPipe,
		DynamicHeadingComponent,
		ContentElementContainerComponent
	],
	templateUrl: './content-element-step.component.html'
})
export class ContentElementStepComponent {
	@Input({ required: true }) index!: number;
	@Input({ required: true }) element!: StepContentElement;
}