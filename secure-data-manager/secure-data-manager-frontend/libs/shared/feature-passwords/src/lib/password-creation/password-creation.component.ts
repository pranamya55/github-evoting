/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators,} from '@angular/forms';
import {TranslateModule} from '@ngx-translate/core';
import {PolicyComponent} from '@sdm/shared-ui-components';
import {BoardMembersComponent} from '../board-members/board-members.component';
import {PasswordAbstractComponent} from '../password-abstract.component';
import {PasswordValidators} from './password.validators';

@Component({
  selector: 'sdm-password-creation',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslateModule,
    BoardMembersComponent,
    PolicyComponent
  ],
  templateUrl: './password-creation.component.html',
})
export class PasswordCreationComponent extends PasswordAbstractComponent {
  policies = [
    'length',
    'digit',
    'specialChar',
    'lowerCaseChar',
    'uppercaseChar',
  ];
  hidePassword = true;
  hideConfirmationPassword = true;

  passwordForm: FormGroup<{
    password: FormControl<string>;
    confirmation: FormControl<string>;
  }>;

  constructor(private readonly fb: FormBuilder) {
    super();

    this.passwordForm = this.fb.nonNullable.group(
      {
        password: [
          '',
          { validators: [Validators.required, PasswordValidators.validate] },
        ],
        confirmation: '',
      },
      { validators: PasswordValidators.confirm },
    );
  }

  get password(): FormControl<string> {
    return this.passwordForm.controls.password;
  }

  hasError(policy: string): boolean {
    return !!this.password.errors?.[policy];
  }

  setPassword() {
    if (this.passwordForm.invalid) return;

    this.registerPassword();
    this.passwordForm.reset();
  }
}
