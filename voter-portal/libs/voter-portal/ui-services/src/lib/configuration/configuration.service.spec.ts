/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';
import { MockProvider } from 'ng-mocks';
import {
	InterpolatableTranslationObject,
	TranslateService,
} from '@ngx-translate/core';
import {
	MockStoreProvider,
	RandomBetween,
	RandomElectionEventId,
	RandomInt,
	RandomString,
	setState,
} from '@vp/shared-util-testing';
import { ExtendedFactor, VoterPortalConfig } from '@vp/voter-portal-util-types';
import { MockStore } from '@ngrx/store/testing';

import { ConfigurationService } from './configuration.service';
import { describe } from 'node:test';

describe('ConfigurationService', () => {
	let service: ConfigurationService;
	let store: MockStore;
	let translate: TranslateService;
	let mockConfig: VoterPortalConfig;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [MockStoreProvider(), MockProvider(TranslateService)],
		});
		service = TestBed.inject(ConfigurationService);
		store = TestBed.inject(MockStore);
		translate = TestBed.inject(TranslateService);

		mockConfig = {
			electionEventId: RandomElectionEventId(),
			identification: RandomBetween(
				ExtendedFactor.DateOfBirth,
				ExtendedFactor.YearOfBirth,
			),
			contestsCapabilities: { writeIns: RandomBetween(true, false) },
			requestTimeout: {
				authenticateVoter: RandomInt(),
				sendVote: RandomInt(),
				confirmVote: RandomInt(),
			},
			header: {
				logo: RandomString(),
				logoHeight: {
					desktop: RandomInt(),
					mobile: RandomInt(),
				},
			},
			translatePlaceholders: {
				placeholder: {
					DE: RandomString(),
					FR: RandomString(),
					IT: RandomString(),
					RM: RandomString(),
				},
			},
			favicon: RandomString(),
		};
	});

	function getTranslations(
		lang: string,
	): Record<string, InterpolatableTranslationObject> {
		return {
			[lang]: { 'translation.key': RandomString() },
		};
	}

	it('should be created', () => {
		expect(service).toBeTruthy();
	});

	it('should initialize properties from store configuration', () => {
		setState(store, { config: mockConfig });

		Object.entries(mockConfig).forEach(([key, value]) => {
			expect(service[key as keyof VoterPortalConfig]).toEqual(value);
		});
	});

	describe('translation placeholders', () => {
		let lang: string;
		let translations: Record<string, InterpolatableTranslationObject>;
		let compiledTranslations: Record<string, InterpolatableTranslationObject>;

		beforeEach(() => {
			lang = 'en';
			translations = getTranslations(lang);
			compiledTranslations = getTranslations(lang);

			translate.currentLang = lang;
			translate.translations = translations;
			translate.compiler = {
				compile: jest.fn(),
				compileTranslations: jest.fn().mockReturnValue(compiledTranslations),
			};

			jest.spyOn(translate, 'setTranslation');
			jest.spyOn(translate.compiler, 'compileTranslations');
		});

		it('should recompile translations when store configuration is received', () => {
			setState(store, { config: mockConfig });

			expect(translate.compiler.compileTranslations).toHaveBeenCalledWith(
				translations,
				lang,
			);
			expect(translate.setTranslation).toHaveBeenCalledWith(
				lang,
				compiledTranslations[lang],
				false,
			);
		});

		it('should not recompile translations if no changes are detected in translations', () => {
			translate.translations = {};
			setState(store, { config: mockConfig });

			expect(translate.compiler.compileTranslations).not.toHaveBeenCalled();
			expect(translate.setTranslation).not.toHaveBeenCalled();
		});
	});
});
