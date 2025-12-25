/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { TestBed } from '@angular/core/testing';

import { AuthoriseDecryptService } from './authorise-decrypt.service';

describe('AuthoriseDecryptService', () => {
  let service: AuthoriseDecryptService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthoriseDecryptService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
