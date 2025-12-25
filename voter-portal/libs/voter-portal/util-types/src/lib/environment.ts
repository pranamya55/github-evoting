/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TranslatableText } from 'e-voting-libraries-ui-kit';

export interface Environment {
	production: boolean;
	defaultLang: keyof TranslatableText;
	availableLanguages: Language[];
	progressOverlayCloseDelay: number | Date;
	progressOverlayNavigateDelay: number | Date;
}

export interface Language {
	id: string;
	label: string;
}
