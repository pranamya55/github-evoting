/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';
import {ContainerContentElement} from "@vp/landing-page-utils-types";

@Component({
	selector: 'vp-content-element-container',
	standalone: true,
	templateUrl: './content-element-container.component.html'
})
export class ContentElementContainerComponent {
	@Input({ required: false }) element!: ContainerContentElement;
}
