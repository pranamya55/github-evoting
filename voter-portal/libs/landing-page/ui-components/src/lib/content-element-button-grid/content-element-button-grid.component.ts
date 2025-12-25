/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';
import {ButtonGridContentElement} from "@vp/landing-page-utils-types";
import {ContentElementButtonComponent} from "../content-element-button/content-element-button.component";

@Component({
	selector: 'vp-content-element-button-grid',
	standalone: true,
	imports: [
		ContentElementButtonComponent
	],
	templateUrl: './content-element-button-grid.component.html'
})
export class ContentElementButtonGridComponent {
	@Input({ required: true }) element!: ButtonGridContentElement;
}
