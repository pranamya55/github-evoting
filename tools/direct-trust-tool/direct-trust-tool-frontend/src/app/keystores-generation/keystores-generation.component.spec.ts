/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';

import {KeystoresGeneration} from './keystores-generation.component';

describe('ComponentSelectionComponent', () => {
  let component: KeystoresGeneration;
  let fixture: ComponentFixture<KeystoresGeneration>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KeystoresGeneration]
    })
      .compileComponents();

    fixture = TestBed.createComponent(KeystoresGeneration);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
