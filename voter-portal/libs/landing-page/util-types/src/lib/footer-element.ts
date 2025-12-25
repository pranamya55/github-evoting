/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ButtonItemContentElement, HeadingContentElement, ParagraphContentElement} from "./content-element";

export type FooterElement =
	| { footerLinks: ButtonItemContentElement[] }
	| { footerContents: ContentFooterElement[][] };

export type ContentFooterElement =
	| { heading: HeadingContentElement }
	| { paragraph: ParagraphContentElement }
	| { link: ButtonItemContentElement };

export function isLinkFooterElement(contentFooterElement: ContentFooterElement): contentFooterElement is { link: ButtonItemContentElement } {
	return 'link' in contentFooterElement;
}

export function isHeadingFooterElement(contentFooterElement: ContentFooterElement): contentFooterElement is { heading: HeadingContentElement } {
	return 'heading' in contentFooterElement;
}

export function isParagraphFooterElement(contentFooterElement: ContentFooterElement): contentFooterElement is { paragraph: ParagraphContentElement } {
	return 'paragraph' in contentFooterElement;
}

export function isFooterLinkFooterElements(footerElement: FooterElement): footerElement is { footerLinks: ButtonItemContentElement[] } {
	return 'footerLinks' in footerElement;
}

export function isFooterContentFooterElements(footerElement: FooterElement): footerElement is { footerContents: ContentFooterElement[][] } {
	return 'footerContents' in footerElement;
}
