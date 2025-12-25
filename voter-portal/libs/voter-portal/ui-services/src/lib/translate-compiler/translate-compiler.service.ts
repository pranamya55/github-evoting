/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { inject, Injectable } from '@angular/core';
import {
	InterpolatableTranslation,
	InterpolatableTranslationObject,
	TranslateCompiler,
	TranslationObject,
} from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import { getConfig } from '@vp/voter-portal-ui-state';
import { TranslatableText } from 'e-voting-libraries-ui-kit';

/**
 * This service injects the placeholders defined by the portal configuration into the translations
 */
@Injectable()
export class SwpTranslateCompiler implements TranslateCompiler {
	private readonly store = inject(Store);
	private readonly config = this.store.selectSignal(getConfig);

	compile(value: string): InterpolatableTranslation {
		return value;
	}

	compileTranslations(
		translations: TranslationObject,
		lang: string,
	): InterpolatableTranslationObject {
		const placeholderRegex = /{{config:([^}]+)}}/g;

		const parsedTranslations: TranslationObject = {};
		Object.entries(translations).forEach(([key, translation]) => {
			if (typeof translation === 'string') {
				parsedTranslations[key] = translation.replace(
					placeholderRegex,
					this.replacePlaceholder(lang.toUpperCase())
				);
			} else {
				parsedTranslations[key] = this.compileTranslations(translation, lang);
			}
		});

		return parsedTranslations;
	}

	private readonly replacePlaceholder =
		(lang: string) => (match: string, placeholderKey: string) => {
			const placeholders = this.config()?.translatePlaceholders;

			if (
				placeholders &&
				placeholderKey in placeholders &&
				lang in placeholders[placeholderKey]
			) {
				return placeholders[placeholderKey][lang as keyof TranslatableText] ?? "";
			}

			return match ?? "";
		};
}
