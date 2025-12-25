/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {DebugElement} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {By, Title} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';
import {NgbCollapseModule} from '@ng-bootstrap/ng-bootstrap';
import {MockStore, provideMockStore} from '@ngrx/store/testing';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '@vp/voter-portal-data-access';
import {FAQService} from '@vp/voter-portal-feature-faq';
import {UiComponentsModule} from '@vp/voter-portal-ui-components';
import {ConfigurationService} from '@vp/voter-portal-ui-services';
import {LanguageSelectorActions} from '@vp/voter-portal-ui-state';
import {RandomArray, RandomElectionEventId, RandomItem, setState} from '@vp/shared-util-testing';
import {ExtendedFactor, Language, VoterPortalConfig} from '@vp/voter-portal-util-types';
import {MockComponent, MockModule, MockProvider} from 'ng-mocks';
import {TranslateTestingModule} from 'ngx-translate-testing';

import {HeaderComponent} from './header.component';
import {IconComponent} from "@vp/shared-ui-components";

// Mock environment configuration
jest.mock('@vp/voter-portal-data-access');
environment.availableLanguages = RandomArray(
	(i) => {
		return {id: `lang-${i}`, label: `Language ${i}`};
	},
	6,
	3
);

describe('HeaderComponent', () => {
	let fixture: ComponentFixture<HeaderComponent>;
	let voterPortalConfig: VoterPortalConfig;

	const defaultLanguage: Language = RandomItem(environment.availableLanguages);

	beforeEach(async () => {
		voterPortalConfig = {
			electionEventId: RandomElectionEventId(),
			identification: ExtendedFactor.YearOfBirth,
			contestsCapabilities: {
				writeIns: true,
			},
			requestTimeout: {
				authenticateVoter: 30000,
				sendVote: 120000,
				confirmVote: 120000
			},
			header: {
				logo: '',
				logoHeight: {desktop: 0, mobile: 0},
			},
			favicon: ''
		};
		await TestBed.configureTestingModule({
			imports: [
				RouterTestingModule,
				TranslateTestingModule.withTranslations({}).withDefaultLanguage(
					defaultLanguage.id
				),
				MockModule(NgbCollapseModule),
				MockModule(UiComponentsModule),
				MockComponent(IconComponent)
			],
			declarations: [HeaderComponent],
			providers: [
				provideMockStore({initialState: {}}),
				MockProvider(FAQService),
				MockProvider(Title),
				MockProvider(ConfigurationService, voterPortalConfig),
			],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(HeaderComponent);
		fixture.detectChanges();
	});

	describe('help', () => {
		let faqService: FAQService;

		beforeEach(() => {
			faqService = TestBed.inject(FAQService);
		});

		it('should open the FAQ with no specific section opened when the help button is clicked', () => {
			const showFAQSpy = jest.spyOn(faqService, 'showFAQ');
			const helpButton: HTMLButtonElement = fixture.debugElement.query(
				By.css('#mainHelpButton')
			).nativeElement;

			helpButton.click();

			expect(showFAQSpy).toHaveBeenNthCalledWith(
				1 /*, called without any arguments*/
			);
		});
	});

	describe('languages', () => {
		let store: MockStore;
		let translate: TranslateService;
		let languageOptions: DebugElement[];
		let newLanguage: Language;

		beforeEach(() => {
			store = TestBed.inject(MockStore);
			translate = TestBed.inject(TranslateService);

			languageOptions = fixture.debugElement.queryAll(
				By.css('#languageSelector > li')
			);

			newLanguage = RandomItem(environment.availableLanguages, (language) => {
				return language.id !== defaultLanguage.id;
			});
		});

		function setConfigLanguage(language?: Language) {
			if (language?.id) setState(store, {currentLanguage: language.id as string});
		}

		it('should show as all languages configured in the environment', () => {
			expect(languageOptions.length).toBe(
				environment.availableLanguages.length
			);
			environment.availableLanguages.forEach((language, i) => {
				expect(languageOptions[i].nativeElement.textContent).toContain(
					language.label
				);
			});
		});

		it('should show the default language as active if there is no language in the current config', () => {
			setConfigLanguage(undefined);

			fixture.detectChanges();

			const activeLanguage = fixture.debugElement.query(
				By.css('#languageSelector .active')
			);
			expect(activeLanguage.nativeElement.textContent).toContain(
				defaultLanguage.label
			);
		});

		it('should show the language from the config as active if there is one', () => {
			setConfigLanguage(newLanguage);

			fixture.detectChanges();

			const activeLanguage = fixture.debugElement.query(
				By.css('#languageSelector .active')
			);
			expect(activeLanguage.nativeElement.textContent).toContain(
				newLanguage.label
			);
		});

		describe('language change', () => {
			let newLanguageButton: HTMLButtonElement;

			beforeEach(() => {
				const newLanguageIndex = environment.availableLanguages.findIndex(
					(language) => {
						return language.id === newLanguage.id;
					}
				);

				newLanguageButton = languageOptions[newLanguageIndex].query(
					By.css('button')
				).nativeElement;
			});

			it('should dispatch a language change action to the store', () => {
				const dispatchSpy = jest.spyOn(store, 'dispatch');

				newLanguageButton.click();

				expect(dispatchSpy).toHaveBeenNthCalledWith(
					1,
					LanguageSelectorActions.languageSelected({lang: newLanguage.id})
				);
			});

			it('should display the selected language as active', () => {
				setConfigLanguage(newLanguage);

				fixture.detectChanges();

				expect(newLanguageButton.classList).toContain('active');
			});

			it('should update the language used by the translate service', () => {
				const translateUseSpy = jest.spyOn(translate, 'use');

				setConfigLanguage(newLanguage);

				expect(translateUseSpy).toHaveBeenNthCalledWith(1, newLanguage.id);
			});

			it('should update the document language', () => {
				setConfigLanguage(newLanguage);

				expect(document.documentElement.lang).toBe(newLanguage.id);
			});
		});
	});
});
