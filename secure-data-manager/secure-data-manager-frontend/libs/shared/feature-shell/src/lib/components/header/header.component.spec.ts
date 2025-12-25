/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {DebugElement} from '@angular/core';
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {NgbDropdownModule} from '@ng-bootstrap/ng-bootstrap';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {environment} from '@sdm/shared-ui-config';
import {RandomIndex, RandomItem} from '@sdm/shared-util-testing';
import {Locale} from '@sdm/shared-util-types';
import {MockModule, MockProvider} from 'ng-mocks';
import {APP_NAME} from '../../app-tokens';
import {HeaderComponent} from './header.component';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;

  let applicationName: string;
  let defaultLocale: Locale;
  let translateService: TranslateService;

  beforeEach(() => {
    applicationName = 'MOCK APPLICATION NAME';
    defaultLocale = RandomItem(environment.locales);

    TestBed.configureTestingModule({
      imports: [
        HeaderComponent,
        MockModule(NgbDropdownModule),
        MockModule(TranslateModule),
      ],
      providers: [
        MockProvider(TranslateService, {
          defaultLang: defaultLocale.id,
        }),
        {
          provide: APP_NAME,
          useValue: applicationName,
        },
      ],
    });

    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;

    translateService = TestBed.inject(TranslateService);

    fixture.detectChanges();
  });

  it('should show the application name', () => {
    const navbarBrand = fixture.debugElement.query(By.css('.navbar-brand'));
    expect(navbarBrand.nativeElement.textContent).toBe(applicationName);
  });

  it('should show the current language', () => {
    const currentLanguage = fixture.debugElement.query(
      By.css('[data-test="currentLanguage"]'),
    );

    expect(currentLanguage).toBeTruthy();
    expect(currentLanguage.nativeElement.textContent).toContain(
      component.currentLocale.id.toUpperCase(),
    );
  });

  describe('initial language', () => {
    let storageGetItemSpy: jest.SpyInstance;
    let navigatorLanguageSpy: jest.SpyInstance;

    beforeEach(() => {
      storageGetItemSpy = jest.spyOn(localStorage['__proto__'], 'getItem');
      navigatorLanguageSpy = jest.spyOn(navigator, 'language', 'get');
    });

    it('should use the stored language if there is one', () => {
      const expectedLocale = environment.locales.find(
        (locale) => locale !== defaultLocale,
      ) as Locale;

      storageGetItemSpy.mockReturnValue(expectedLocale.id);
      component.ngOnInit();

      expect(component.currentLocale).toBe(expectedLocale);
    });

    it('should use the navigator language if there is a matching locale', () => {
      const expectedLocale = environment.locales.find(
        (locale) => locale !== defaultLocale,
      ) as Locale;

      storageGetItemSpy.mockReturnValue(null);
      navigatorLanguageSpy.mockReturnValue(expectedLocale.id);
      component.ngOnInit();

      expect(component.currentLocale).toBe(expectedLocale);
    });

    it('should use the default language if there is no stored language and no locale matching the navigator language', () => {
      storageGetItemSpy.mockReturnValue(null);
      navigatorLanguageSpy.mockReturnValue('sw');
      component.ngOnInit();

      expect(component.currentLocale.id).toBe(defaultLocale.id);
    });
  });

  describe('language options', () => {
    let localeOptions: Locale[];
    let languageLinks: DebugElement[];

    beforeEach(() => {
      localeOptions = environment.locales.filter(
        (locale) => locale !== component.currentLocale,
      );
      languageLinks = fixture.debugElement.queryAll(
        By.css('[data-test="language"]'),
      );
    });

    function selectLanguage() {
      const randomIndex = RandomIndex(languageLinks);
      languageLinks.at(randomIndex)?.nativeElement.click();
      fixture.detectChanges();
      return localeOptions.at(randomIndex) as Locale;
    }

    it('should show all language options', () => {
      expect(languageLinks.length).toBe(localeOptions.length);
      languageLinks.forEach((languageLink, i) => {
        expect(languageLink.nativeElement.textContent).toContain(
          localeOptions.at(i)?.id.toUpperCase(),
        );
      });
    });

    it('should have a "title" and a "lang" attribute on all language options', () => {
      languageLinks.forEach((languageLink, i) => {
        const locale = localeOptions.at(i) as Locale;

        expect(languageLink.attributes['title']).toBe(locale.name);
        expect(languageLink.attributes['lang']).toBe(locale.id);
      });
    });

    it('should change the current locale on click', () => {
      const translateUseSpy = jest.spyOn(translateService, 'use');
      const selectedLocale = selectLanguage();

      expect(component.currentLocale).toBe(selectedLocale);
      expect(translateUseSpy).toHaveBeenNthCalledWith(1, selectedLocale.id);
    });

    it('should update the document "lang" attribute on click', () => {
      const selectedLocale = selectLanguage();
      expect(document.documentElement.lang).toBe(selectedLocale.id);
    });

    it('should store the selected locale in the local storage', () => {
      const storageSetItemSpy = jest.spyOn(
        localStorage['__proto__'],
        'setItem',
      );
      const selectedLocale = selectLanguage();

      expect(storageSetItemSpy).toHaveBeenNthCalledWith(
        1,
        expect.any(String),
        selectedLocale.id,
      );
    });
  });
});
