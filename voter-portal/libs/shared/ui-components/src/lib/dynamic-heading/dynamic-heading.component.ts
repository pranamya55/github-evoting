/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, Input } from '@angular/core';
import {NgTemplateOutlet} from "@angular/common";

@Component({
	selector: 'vp-dynamic-heading',
	standalone: true,
	imports: [
		NgTemplateOutlet
	],
	templateUrl: './dynamic-heading.component.html'
})
export class DynamicHeadingComponent {
	@Input({ required: true }) level!: number;
}
