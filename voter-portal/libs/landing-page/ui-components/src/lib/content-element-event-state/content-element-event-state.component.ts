/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {NgClass} from "@angular/common";
import {Component, Input} from '@angular/core';
import {EventStateContentElement} from "@vp/landing-page-utils-types";
import {MarkdownPipe, TranslateTextPipe} from "e-voting-libraries-ui-kit";

@Component({
	selector: 'vp-content-element-event-state',
	standalone: true,
	imports: [
		MarkdownPipe,
		TranslateTextPipe,
		NgClass
	],
	templateUrl: './content-element-event-state.component.html'
})
export class ContentElementEventStateComponent {
	@Input({required: true}) element!: EventStateContentElement;
}