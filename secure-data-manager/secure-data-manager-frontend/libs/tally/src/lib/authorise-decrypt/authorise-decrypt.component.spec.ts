/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthoriseDecryptComponent } from './authorise-decrypt.component';

describe('AuthoriseDecryptComponent', () => {
  let component: AuthoriseDecryptComponent;
  let fixture: ComponentFixture<AuthoriseDecryptComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthoriseDecryptComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AuthoriseDecryptComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
