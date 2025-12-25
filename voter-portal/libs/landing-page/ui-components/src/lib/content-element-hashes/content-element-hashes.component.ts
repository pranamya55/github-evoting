/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';
import {HashContentElement} from "@vp/landing-page-utils-types";
import {TranslatePipe} from "@ngx-translate/core";

@Component({
	selector: 'vp-content-element-hashes',
	standalone: true,
	imports: [
		TranslatePipe
	],
	templateUrl: './content-element-hashes.component.html'
})
export class ContentElementHashesComponent {
	@Input({ required: true }) element!: HashContentElement[];
}
