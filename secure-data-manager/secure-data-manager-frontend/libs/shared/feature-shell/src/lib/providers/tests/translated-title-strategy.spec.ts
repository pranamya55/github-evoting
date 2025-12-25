/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {TestBed} from '@angular/core/testing';
import {Title} from '@angular/platform-browser';
import {RouterStateSnapshot} from '@angular/router';
import {TranslateService} from '@ngx-translate/core';
import {MockProvider} from 'ng-mocks';
import {EMPTY, of, Subscription} from 'rxjs';
import {TranslatedTitleStrategy} from '../translated-title-strategy';
import {APP_NAME} from "@sdm/shared-util-types";

describe('TranslatedTitleStrategy', () => {
  const snapshot = { url: '/some/url' } as RouterStateSnapshot;
  const appName = 'Mock App Name';

  let translatedTitleStrategy: TranslatedTitleStrategy;
  let titleService: Title;
  let translateService: TranslateService;

  let buildTitleSpy: jest.SpyInstance;
  let setTitleSpy: jest.SpyInstance;
  let streamSpy: jest.SpyInstance;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TranslatedTitleStrategy,
        MockProvider(Title),
        MockProvider(TranslateService),
        {
          provide: APP_NAME,
          useValue: appName,
        },
      ],
    });

    translatedTitleStrategy = TestBed.inject(TranslatedTitleStrategy);
    titleService = TestBed.inject(Title);
    translateService = TestBed.inject(TranslateService);

    buildTitleSpy = jest.spyOn(translatedTitleStrategy, 'buildTitle');
    setTitleSpy = jest.spyOn(titleService, 'setTitle');
    streamSpy = jest.spyOn(translateService, 'stream');

    streamSpy.mockReturnValue(EMPTY);
  });

  function runUpdateTitle() {
    translatedTitleStrategy.updateTitle(snapshot);
  }

  it('should unsubscribe the current subscription if there is one', () => {
    translatedTitleStrategy.subscription = new Subscription();
    const unsubscribeSpy = jest.spyOn(
      translatedTitleStrategy.subscription,
      'unsubscribe',
    );

    runUpdateTitle();

    expect(unsubscribeSpy).toHaveBeenCalledTimes(1);
  });

  it('should call the buildTitle method with the current snapshot', () => {
    runUpdateTitle();
    expect(buildTitleSpy).toHaveBeenNthCalledWith(1, snapshot);
  });

  it('should not create a new subscription if the title key is undefined', () => {
    buildTitleSpy.mockReturnValueOnce(undefined);
    runUpdateTitle();
    expect(translatedTitleStrategy.subscription).toBeUndefined();
  });

  it('should use the app name as tab title if the title key is undefined', () => {
    buildTitleSpy.mockReturnValueOnce(undefined);
    runUpdateTitle();
    expect(setTitleSpy).toHaveBeenNthCalledWith(1, appName);
  });

  it('should create a new subscription if the title key is defined', () => {
    expect(translatedTitleStrategy.subscription).toBeUndefined();

    const titleKey = 'mock.title.key';
    buildTitleSpy.mockReturnValueOnce(titleKey);
    runUpdateTitle();

    expect(streamSpy).toHaveBeenNthCalledWith(1, titleKey);
    expect(translatedTitleStrategy.subscription).not.toBeUndefined();
  });

  it('should set the app name and the page title as the browser tab title', () => {
    const expectedTitle = 'Mock Title';

    streamSpy.mockReturnValueOnce(of(expectedTitle));
    buildTitleSpy.mockReturnValueOnce('mock.title.key');
    runUpdateTitle();

    expect(setTitleSpy).toHaveBeenNthCalledWith(
      1,
      `${appName} | ${expectedTitle}`,
    );
  });
});
