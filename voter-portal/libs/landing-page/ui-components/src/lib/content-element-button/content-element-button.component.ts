/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';
import {NgTemplateOutlet} from "@angular/common";
import {ButtonItemContentElement} from "@vp/landing-page-utils-types";
import {ContentElementLinkComponent} from "../content-element-link/content-element-link.component";

@Component({
	selector: 'vp-content-element-button',
	standalone: true,
	imports: [
		NgTemplateOutlet,
		ContentElementLinkComponent
	],
	templateUrl: './content-element-button.component.html'
})
export class ContentElementButtonComponent {
	@Input({ required: true }) element!: ButtonItemContentElement;
	@Input({ required: false }) secondary: boolean = false;
	@Input({ required: false }) grid: boolean = false;
	@Input({ required: false }) disabled: boolean = false;
}
