/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';

import {PublicKeysHashesComponent} from './public-keys-hashes.component';

describe('KeystoreDownloadComponent', () => {
  let component: PublicKeysHashesComponent;
  let fixture: ComponentFixture<PublicKeysHashesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicKeysHashesComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(PublicKeysHashesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
