/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, Input } from '@angular/core';
import {NgTemplateOutlet} from "@angular/common";
import {IconComponent} from "@vp/shared-ui-components";

@Component({
	selector: 'vp-content-element-alert',
	standalone: true,
	templateUrl: './content-element-alert.component.html',
	imports: [
		NgTemplateOutlet,
		IconComponent
	]
})
export class ContentElementAlertComponent {
	@Input({ required: true }) level!: string;
}
