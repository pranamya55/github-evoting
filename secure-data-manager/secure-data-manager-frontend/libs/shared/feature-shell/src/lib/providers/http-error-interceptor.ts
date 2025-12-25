/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpStatusCode,} from '@angular/common/http';
import {ErrorHandler, Injectable} from '@angular/core';
import {catchError, Observable, of, throwError} from 'rxjs';

@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
	constructor(private readonly errorHandler: ErrorHandler) {
	}

	intercept(
		request: HttpRequest<unknown>,
		next: HttpHandler,
	): Observable<HttpEvent<unknown>> {
		return next.handle(request).pipe(
			catchError((error: HttpErrorResponse) => {
				if (error.status === HttpStatusCode.BadRequest) {
					// By convention this is an expected error, so we let the caller handle it.
					return throwError(() => error);
				} else {
					this.errorHandler.handleError(error);
					return of();
				}
			}),
		);
	}
}
