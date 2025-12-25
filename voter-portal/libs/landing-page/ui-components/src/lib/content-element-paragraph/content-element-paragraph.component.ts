/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, Input } from '@angular/core';
import {ParagraphContentElement} from "@vp/landing-page-utils-types";
import {MarkdownPipe, TranslateTextPipe} from "e-voting-libraries-ui-kit";

@Component({
	selector: 'vp-content-element-paragraph',
	standalone: true,
	imports: [
		TranslateTextPipe,
		MarkdownPipe
	],
	templateUrl: './content-element-paragraph.component.html'
})
export class ContentElementParagraphComponent {
	@Input({ required: true }) element!: ParagraphContentElement;
}
