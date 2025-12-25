/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {HttpErrorResponse, HttpHandler, HttpRequest, HttpResponse, HttpStatusCode,} from '@angular/common/http';
import {ErrorHandler} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {RandomItem} from '@sdm/shared-util-testing';
import {MockProvider} from 'ng-mocks';
import {of, throwError} from 'rxjs';

import {HttpErrorInterceptor} from '../http-error-interceptor';

describe('HttpErrorInterceptor', () => {
  let mockRequest: HttpRequest<unknown>;
  let mockErrorHandler: ErrorHandler;
  let mockHttpHandler: HttpHandler;

  let httpErrorInterceptor: HttpErrorInterceptor;

  beforeEach(() => {
    mockRequest = new HttpRequest(
      RandomItem(['DELETE', 'JSONP', 'OPTIONS']),
      '',
    );

    TestBed.configureTestingModule({
      providers: [
        HttpErrorInterceptor,
        MockProvider(ErrorHandler, {
          handleError: jest.fn(),
        }),
        MockProvider(HttpHandler, {
          handle: jest.fn(),
        }),
      ],
    });

    mockErrorHandler = TestBed.inject(ErrorHandler);
    mockHttpHandler = TestBed.inject(HttpHandler);

    httpErrorInterceptor = TestBed.inject(HttpErrorInterceptor);
  });

  it('should not intercept http response with status 200 (OK)', (done) => {
    const mockResponse = new HttpResponse({ body: 'success response' });
    (mockHttpHandler.handle as jest.Mock).mockReturnValue(of(mockResponse));

    const intercept = httpErrorInterceptor.intercept(
      mockRequest,
      mockHttpHandler,
    );

    intercept.subscribe({
      next: (response) => {
        expect(response).toBe(mockResponse);
      },
      error: () => done.fail('no error'),
      complete: done,
    });
  });

  it('should not intercept http error response with status 400 (BadRequest)', (done) => {
    const mockErrorResponse = new HttpErrorResponse({
      status: HttpStatusCode.BadRequest,
    });
    (mockHttpHandler.handle as jest.Mock).mockReturnValue(
      throwError(() => mockErrorResponse),
    );

    const intercept = httpErrorInterceptor.intercept(
      mockRequest,
      mockHttpHandler,
    );

    intercept.subscribe({
      next: () => done.fail('no value emitted'),
      error: (error) => {
        expect(error).toBe(mockErrorResponse);
        done();
      },
      complete: () => done.fail('should throw, not complete'),
    });
  });

  it('should intercept http error response with status other than 400 and pass it to the error handler', (done) => {
    const mockErrorResponse = new HttpErrorResponse({
      status: HttpStatusCode.NotFound,
    });
    (mockHttpHandler.handle as jest.Mock).mockReturnValue(
      throwError(() => mockErrorResponse),
    );

    const intercept = httpErrorInterceptor.intercept(
      mockRequest,
      mockHttpHandler,
    );

    intercept.subscribe({
      next: () => done.fail('no value emitted'),
      error: () => done.fail('no error'),
      complete: () => {
        expect(mockErrorHandler.handleError).toHaveBeenCalledTimes(1);
        done();
      },
    });
  });
});
