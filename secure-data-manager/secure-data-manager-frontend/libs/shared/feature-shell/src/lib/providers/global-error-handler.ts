/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {ErrorHandler, Injectable} from '@angular/core';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  constructor() {}

  handleError(error?: unknown) {
    if (error) {
      console.error(error);
    }
  }
}
