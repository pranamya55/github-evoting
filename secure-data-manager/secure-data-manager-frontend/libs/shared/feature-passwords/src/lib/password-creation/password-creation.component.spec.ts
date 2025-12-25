/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ComponentFixture, TestBed} from '@angular/core/testing';
import {TranslateModule} from '@ngx-translate/core';
import {MockComponent, MockModule} from 'ng-mocks';
import {PasswordCreationComponent} from './password-creation.component';
import {BoardMembersComponent} from '../board-members/board-members.component';
import {FormBuilder} from '@angular/forms';
import {PolicyComponent} from '@sdm/shared-ui-components';
import {MockBoardMember, RandomArray} from '@sdm/shared-util-testing';
import {DebugElement} from '@angular/core';
import {PasswordValidators} from './password.validators';
import {By} from '@angular/platform-browser';
import {BoardMember} from '@sdm/shared-util-types';

describe('PasswordCreationComponent', () => {
  let component: PasswordCreationComponent;
  let fixture: ComponentFixture<PasswordCreationComponent>;

  let passwordForm: DebugElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        PasswordCreationComponent,
        MockModule(TranslateModule),
        MockComponent(BoardMembersComponent),
        MockComponent(PolicyComponent),
      ],
      providers: [FormBuilder],
    }).compileComponents();

    fixture = TestBed.createComponent(PasswordCreationComponent);
    component = fixture.componentInstance;

    component.boardMembers = RandomArray(MockBoardMember, 3);

    // make password form valid
    component.password.setValue('Password');
    jest.spyOn(PasswordValidators, 'validate').mockReturnValue(null);
    jest.spyOn(PasswordValidators, 'confirm').mockReturnValue(null);

    fixture.detectChanges();

    passwordForm = fixture.debugElement.query(By.css('[data-test="form"]'));
  });

  function getPolicy(policy: string): DebugElement {
    return fixture.debugElement.query(By.css(`[data-test="policy-${policy}"]`));
  }

  it('should set an active member on init', () => {
    expect(component.activeMember).toBeTruthy();
  });

  it('should set a password for the active member when the form is submitted', () => {
    const activeMember = component.activeMember as BoardMember;
    const password = 'Mock password';

    component.password.setValue(password);
    passwordForm.nativeElement.submit();

    expect(component.passwords.get(activeMember.id)).toBe(password);
  });

  it('should set another member as active member when the form is submitted', () => {
    const previousActiveMember = component.activeMember as BoardMember;

    passwordForm.nativeElement.submit();

    expect(component.activeMember).toBeTruthy();
    expect(previousActiveMember.id).not.toBe(component.activeMember?.id);
  });

  it('should reset the form when it is submitted', () => {
    expect(component.password.value).toBeTruthy();

    passwordForm.nativeElement.submit();

    expect(component.password.value).toBeFalsy();
  });

  it('should set the password policies has met if the password control is valid', () => {
    component.policies.forEach((policy) => {
      expect(getPolicy(policy).componentInstance.isMet).toBeTruthy();
    });
  });

  it('should set the password policies has unmet if the password has the corresponding error', () => {
    component.policies.forEach((policy) => {
      jest
        .spyOn(PasswordValidators, 'validate')
        .mockReturnValue({ [policy]: true });

      component.password.updateValueAndValidity();
      fixture.detectChanges();

      expect(getPolicy(policy).componentInstance.isMet).toBeFalsy();
    });
  });

  it('should set the confirmation policy has met if the form is valid', () => {
    expect(getPolicy('confirmation').componentInstance.isMet).toBeTruthy();
  });

  it('should set the confirmation policy has unmet if the form is not valid', () => {
    jest
      .spyOn(PasswordValidators, 'confirm')
      .mockReturnValue({ confirmation: true });

    component.passwordForm.updateValueAndValidity();
    fixture.detectChanges();

    expect(getPolicy('confirmation').componentInstance.isMet).toBeFalsy();
  });
});
