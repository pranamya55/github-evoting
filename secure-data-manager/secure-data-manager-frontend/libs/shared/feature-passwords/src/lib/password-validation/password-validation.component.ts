/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component, ElementRef, EventEmitter, Input, Output, ViewChild,} from '@angular/core';
import {FormBuilder, FormControl, FormsModule, NgForm, ReactiveFormsModule, Validators,} from '@angular/forms';
import {TranslateModule} from '@ngx-translate/core';
import {BoardMember} from '@sdm/shared-util-types';
import {Subject, take} from 'rxjs';
import {BoardMembersComponent} from '../board-members/board-members.component';
import {PasswordAbstractComponent} from '../password-abstract.component';

@Component({
  selector: 'sdm-password-validation',
  standalone: true,
  imports: [
    CommonModule,
    BoardMembersComponent,
    FormsModule,
    ReactiveFormsModule,
    TranslateModule,
  ],
  templateUrl: './password-validation.component.html',
})
export class PasswordValidationComponent extends PasswordAbstractComponent {
  @ViewChild('form') passwordForm!: NgForm;
  @ViewChild('input') passwordInput?: ElementRef<HTMLInputElement>;

  @Input({ required: true }) isPasswordValid!: Subject<boolean>;
  @Output() passwordSet = new EventEmitter<[BoardMember, string]>();

  password: FormControl<string>;
  isValidating = false;
  @Input({ required: false }) enabled: boolean = true;
  hideConfirmationPassword = true;

  constructor(private readonly fb: FormBuilder) {
    super();

    this.password = this.fb.nonNullable.control('', {
      validators: [Validators.required],
    });
  }

  setPassword() {
    if (!this.activeMember || this.password.invalid) return;

    this.passwordSet.emit([this.activeMember, this.password.value]);

    this.isValidating = true;
    this.password.disable();
    this.isPasswordValid.pipe(take(1)).subscribe((wasValidated) => {
      this.isValidating = false;
      this.password.enable();
      if (!wasValidated) {
        this.password.setErrors({ invalid: true });
        return;
      }

      this.registerPassword();

      this.password.reset();
      this.markFormAsNotSubmitted();
    });
  }

  markFormAsNotSubmitted() {
    if (this.passwordForm.submitted) this.passwordForm.resetForm();
  }
}
