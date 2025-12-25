/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {TranslatableText} from "e-voting-libraries-ui-kit";
import {BootstrapIconName} from "@vp/shared-ui-components";

export type ContentElement =
	| { accordion: AccordionContentElement }
	| { alert: AlertContentElement }
	| { button: ButtonItemContentElement }
	| { buttonGrid: ButtonGridContentElement }
	| { captcha: CaptchaContentElement }
	| { container: ContainerContentElement }
	| { eventReference: EventReferenceContentElement }
	| { eventState: EventStateContentElement }
	| { hashes: HashContentElement[] }
	| { heading: HeadingContentElement }
	| { paragraph: ParagraphContentElement }
	| { sharedReference: SharedReferenceContentElement }
	| { steps: StepContentElement[] };

// 'accordion'
export interface AccordionContentElement {
	level: 1 | 2 | 3 | 4 | 5 | 6;
	title: TranslatableText;
	icon: BootstrapIconName;
	contentElements: ContentElement[];
}

// 'alert'
export interface AlertContentElement {
	level: "info" | "warning" | "error" | "success";
	contentElements: ContentElement[];
}

// 'button'
export interface ButtonItemContentElement {
	text: TranslatableText;
	url: string;
	icon?: BootstrapIconName;
	addLanguageParameter?: boolean;
	languageParameter?: string;
	main?: boolean;
}

// 'buttonGrid'
export type ButtonGridContentElement =
	| { primary: ButtonItemContentElement; secondary?: ButtonItemContentElement }
	| { primary?: ButtonItemContentElement; secondary: ButtonItemContentElement };

// 'captcha'
export interface CaptchaContentElement {
	url: string;
	captchaResponseParameterName: string;
	instructions: TranslatableText;
	button: ButtonItemContentElement;
}

// 'container'
export interface ContainerContentElement {
	logo?: string
	contentElements: ContentElement[];
}

// 'eventReference'
export interface EventReferenceContentElement {
	id: string;
	contentElements?: ContentElement[]; // Filled by the EventPhaseEventElement content elements.
}

// 'eventState'
export interface EventStateContentElement {
	phaseLevel: EventPhaseLevel;
	text: TranslatableText;
}

// 'hashes'
export interface HashContentElement {
	file: string;
	hash: string;
}

// 'heading'
export interface HeadingContentElement {
	level: 1 | 2 | 3 | 4 | 5 | 6;
	text: TranslatableText;
}

// 'paragraph'
export interface ParagraphContentElement {
	text: TranslatableText;
}

// 'sharedReference'
export interface SharedReferenceContentElement {
	id: string;
	contentElements?: ContentElement[]; // Filled by the SharedContentElement content elements.
}

// 'steps'
export interface StepContentElement {
	title: TranslatableText;
	contentElements: ContentElement[]
}

type EventPhaseLevel = "UPCOMING" | "OPEN" | "EVOTING-CLOSED" | "CLOSED";

export interface EventElement {
	id: string;
	phases: EventPhaseElement[];
}

export interface EventPhaseElement {
	id: string;
	phaseLevel: EventPhaseLevel;
	active: boolean;
	contentElements?: ContentElement[];
}

export interface SharedContentElement {
	id: string;
	contentElements: ContentElement[];
}

export function isAccordionContentElement(contentElement: ContentElement): contentElement is { accordion: AccordionContentElement } {
	return 'accordion' in contentElement;
}

export function isAlertContentElement(contentElement: ContentElement): contentElement is { alert: AlertContentElement } {
	return 'alert' in contentElement;
}

export function isButtonContentElement(contentElement: ContentElement): contentElement is { button: ButtonItemContentElement } {
	return 'button' in contentElement;
}

export function isButtonGridContentElement(contentElement: ContentElement): contentElement is { buttonGrid: ButtonGridContentElement } {
	return 'buttonGrid' in contentElement;
}

export function isCaptchaContentElement(contentElement: ContentElement): contentElement is { captcha: CaptchaContentElement } {
	return 'captcha' in contentElement;
}

export function isContainerContentElement(contentElement: ContentElement): contentElement is { container: ContainerContentElement } {
	return 'container' in contentElement;
}

export function isEventReferenceContentElement(contentElement: ContentElement): contentElement is { eventReference: EventReferenceContentElement } {
	return 'eventReference' in contentElement;
}

export function isEventStateContentElement(contentElement: ContentElement): contentElement is { eventState: EventStateContentElement } {
	return 'eventState' in contentElement;
}

export function isHashesContentElement(contentElement: ContentElement): contentElement is { hashes: HashContentElement[] } {
	return 'hashes' in contentElement;
}

export function isHeadingContentElement(contentElement: ContentElement): contentElement is { heading: HeadingContentElement } {
	return 'heading' in contentElement;
}

export function isParagraphContentElement(contentElement: ContentElement): contentElement is { paragraph: ParagraphContentElement } {
	return 'paragraph' in contentElement;
}

export function isSharedReferenceContentElement(contentElement: ContentElement): contentElement is { sharedReference: SharedReferenceContentElement } {
	return 'sharedReference' in contentElement;
}

export function isStepsContentElement(contentElement: ContentElement): contentElement is { steps: StepContentElement[] } {
	return 'steps' in contentElement;
}