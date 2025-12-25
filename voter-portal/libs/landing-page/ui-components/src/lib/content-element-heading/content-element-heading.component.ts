/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';
import {HeadingContentElement} from "@vp/landing-page-utils-types";
import {DynamicHeadingComponent} from "@vp/shared-ui-components";
import {MarkdownPipe, TranslateTextPipe} from "e-voting-libraries-ui-kit";

@Component({
	selector: 'vp-content-element-heading',
	standalone: true,
	imports: [
		TranslateTextPipe,
		DynamicHeadingComponent,
		MarkdownPipe
	],
	templateUrl: './content-element-heading.component.html'
})
export class ContentElementHeadingComponent {
	@Input({ required: true }) element!: HeadingContentElement;
}
