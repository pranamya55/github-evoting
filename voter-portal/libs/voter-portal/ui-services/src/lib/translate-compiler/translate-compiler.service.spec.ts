/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';
import {
	MockStoreProvider,
	RandomItem,
	RandomString,
	setState,
} from '@vp/shared-util-testing';
import { MockStore } from '@ngrx/store/testing';
import { VoterPortalConfig } from '@vp/voter-portal-util-types';

import { SwpTranslateCompiler } from './translate-compiler.service';

describe('TranslateCompilerService', () => {
	let service: SwpTranslateCompiler;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [SwpTranslateCompiler, MockStoreProvider()],
		});
		service = TestBed.inject(SwpTranslateCompiler);
	});

	describe('compile', () => {
		it('should return the value as-is', () => {
			const value = 'mockValue';
			expect(service.compile(value)).toBe(value);
		});
	});

	describe('compileTranslations', () => {
		let lang: string;
		let store: MockStore;
		let mockPlaceholder: string;

		beforeEach(() => {
			store = TestBed.inject(MockStore);

			lang = RandomItem(['DE', 'FR', 'IT', 'EN']);
			mockPlaceholder = RandomString();

			setPlaceholders();
		});

		function setPlaceholders(
			placeholders = { placeholder: { [lang]: mockPlaceholder } },
		) {
			setState(store, {
				config: {
					translatePlaceholders: placeholders,
				} as unknown as VoterPortalConfig,
			});
		}

		it('should return the translations as-is if no placeholder is found for current language', () => {
			const translations = { mockTranslation: 'mockTranslation' };

			setPlaceholders(undefined);

			expect(service.compileTranslations(translations, lang)).toEqual(
				translations,
			);
		});

		it('should inject the placeholder from the configuration service into the translations', () => {
			const translations = {
				mockTranslation: 'I am a translation with a {{config:placeholder}}',
			};
			const expectedTranslations = {
				mockTranslation: `I am a translation with a ${mockPlaceholder}`,
			};

			expect(service.compileTranslations(translations, lang)).toEqual(
				expectedTranslations,
			);
		});

		it('should not replace the translation params', () => {
			const translations = {
				mockTranslation: 'I am a translation with a {{translationParam}}',
			};

			expect(service.compileTranslations(translations, lang)).toEqual(
				translations,
			);
		});

		it('should work with nested translations', () => {
			const translations = {
				mockTranslationKey: {
					mockNestedTranslationKey:
						'I am a nested translation with a {{config:placeholder}}',
				},
			};
			const expectedTranslations = {
				mockTranslationKey: {
					mockNestedTranslationKey: `I am a nested translation with a ${mockPlaceholder}`,
				},
			};

			expect(service.compileTranslations(translations, lang)).toEqual(
				expectedTranslations,
			);
		});
	});
});
