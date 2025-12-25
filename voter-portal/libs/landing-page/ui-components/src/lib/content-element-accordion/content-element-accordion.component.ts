/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';
import {AccordionContentElement} from "@vp/landing-page-utils-types";
import {TranslateTextPipe} from "e-voting-libraries-ui-kit";
import {AccordionComponent, IconComponent} from "@vp/shared-ui-components";

@Component({
	selector: 'vp-content-element-accordion',
	standalone: true,
	imports: [
		TranslateTextPipe,
		AccordionComponent,
		IconComponent
	],
	templateUrl: './content-element-accordion.component.html'
})
export class ContentElementAccordionComponent {
	@Input({ required: true }) element!: AccordionContentElement;
}
