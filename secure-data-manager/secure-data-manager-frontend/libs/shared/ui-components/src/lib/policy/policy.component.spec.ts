/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {PolicyComponent} from './policy.component';
import {MockModule, MockPipe} from 'ng-mocks';
import {TranslateModule, TranslatePipe} from '@ngx-translate/core';
import {By} from '@angular/platform-browser';
import {DebugElement} from '@angular/core';

describe('PolicyComponent', () => {
  let component: PolicyComponent;
  let fixture: ComponentFixture<PolicyComponent>;

  let label: DebugElement;
  let indicator: DebugElement;
  let ariaLabel: DebugElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [MockPipe(TranslatePipe, (val) => val)],
      imports: [PolicyComponent, MockModule(TranslateModule)],
    }).compileComponents();

    fixture = TestBed.createComponent(PolicyComponent);
    component = fixture.componentInstance;

    label = fixture.debugElement.query(By.css('[data-test="label"]'));
    indicator = fixture.debugElement.query(By.css('[data-test="indicator"]'));
    ariaLabel = fixture.debugElement.query(By.css('[data-test="aria-label"]'));

    fixture.detectChanges();
  });

  it('should show the provided label', () => {
    component.label = 'Mock Policy Label';
    fixture.detectChanges();

    expect(label.nativeElement.textContent).toContain(component.label);
  });

  it('should show a circle if the policy is not met', () => {
    component.isMet = false;
    fixture.detectChanges();

    expect(indicator.classes['bi-circle']).toBeTruthy();
    expect(indicator.classes['bi-check2-circle']).toBeFalsy();
  });

  it('should show a circle with a check mark if the policy is met', () => {
    component.isMet = true;
    fixture.detectChanges();

    expect(indicator.classes['bi-circle']).toBeFalsy();
    expect(indicator.classes['bi-check2-circle']).toBeTruthy();
  });

  it('should not show an "is met" label if the policy is not met', () => {
    component.isMet = false;
    fixture.detectChanges();

    expect(ariaLabel.nativeElement.textContent).not.toContain(
      'components.policy.met',
    );
  });

  it('should show an "is met" label if the policy is met', () => {
    component.isMet = true;
    fixture.detectChanges();

    expect(ariaLabel.nativeElement.textContent).toContain(
      'components.policy.met',
    );
  });
});
