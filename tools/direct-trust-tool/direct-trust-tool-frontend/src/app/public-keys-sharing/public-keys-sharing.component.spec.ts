/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';

import {PublicKeysSharingComponent} from './public-keys-sharing.component';

describe('PublicKeyImportComponent', () => {
  let component: PublicKeysSharingComponent;
  let fixture: ComponentFixture<PublicKeysSharingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicKeysSharingComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(PublicKeysSharingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
