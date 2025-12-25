/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Injector} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {ToastService} from '@sdm/shared-ui-services';

import {GlobalErrorHandler} from '../global-error-handler';

describe('GlobalErrorHandler', () => {
  let mockToastService: ToastService;
  let mockInjector: Injector;

  let globalErrorHandler: GlobalErrorHandler;

  beforeEach(() => {
    mockToastService = {
      error: jest.fn(),
    } as unknown as ToastService;

    TestBed.configureTestingModule({
      providers: [
        GlobalErrorHandler,
        {
          provide: Injector,
          useFactory: () => ({
            get: jest.fn().mockReturnValue(mockToastService),
          }),
        },
      ],
    });

    mockInjector = TestBed.inject(Injector);

    globalErrorHandler = TestBed.inject(GlobalErrorHandler);
  });

  it('should inject the toaster service if not already injected', () => {
    globalErrorHandler.handleError();

    expect(mockInjector.get).toHaveBeenNthCalledWith(1, ToastService);
  });

  it('should not inject the toaster service if already injected', () => {
    globalErrorHandler['_toastService'] = mockToastService as ToastService;

    globalErrorHandler.handleError();

    expect(mockInjector.get).not.toHaveBeenCalled();
  });

  it('should show a generic error', () => {
    globalErrorHandler.handleError();

    expect(mockToastService.error).toHaveBeenNthCalledWith(1, 'error.generic');
  });
});
