/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';
import {ContentElementLinkComponent} from "../content-element-link/content-element-link.component";
import {ContentElementHeadingComponent} from "../content-element-heading/content-element-heading.component";
import {ContentElementParagraphComponent} from "../content-element-paragraph/content-element-paragraph.component";
import {
	FooterElement,
	isFooterContentFooterElements,
	isFooterLinkFooterElements,
	isHeadingFooterElement,
	isLinkFooterElement,
	isParagraphFooterElement
} from "@vp/landing-page-utils-types";

@Component({
	selector: 'vp-footer-element',
	imports: [ContentElementLinkComponent, ContentElementHeadingComponent, ContentElementParagraphComponent],
	templateUrl: './footer-element.component.html',
})
export class FooterElementComponent {
	@Input({required:true}) footerElement!: FooterElement;
	protected readonly isFooterContentFooterElements = isFooterContentFooterElements;
	protected readonly isFooterLinkFooterElements = isFooterLinkFooterElements;
	protected readonly isLinkFooterElement = isLinkFooterElement;
	protected readonly isHeadingFooterElement = isHeadingFooterElement;
	protected readonly isParagraphFooterElement = isParagraphFooterElement;
}
