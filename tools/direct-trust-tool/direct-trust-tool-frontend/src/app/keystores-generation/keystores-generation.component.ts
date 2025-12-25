/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, effect} from '@angular/core';
import {CommonModule} from '@angular/common';
import {AbstractControl, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators} from "@angular/forms";
import {HttpClient} from '@angular/common/http';
import {SessionService} from '../session/session.service';
import {API_BASE_PATH} from '../app.module';
import {switchMap} from 'rxjs';
import {OrganisationValidator} from './organisation.validator';
import {ConfigurationService} from "../configuration/configuration.service";

@Component({
  selector: 'app-keystores-generation',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './keystores-generation.component.html',
  styleUrl: './keystores-generation.component.css'
})
export class KeystoresGeneration {

  generationForm: FormGroup;
  generationInProgress: boolean = false;
  organisationPolicies: string[] = ['prefix', 'contentPrefix', 'date', 'suffix', 'delimiter'];
  organisationPoliciesText: Map<string, string> = new Map<string, string>(
    [['prefix', 'A fixed prefix DT.'],
      ['contentPrefix', 'CT: cantonal abbreviation, for example SG, TG...'],
      ['date', 'Date in \'YYYYMMDD\' format.'],
      ['suffix', 'XY01: Test (TT) / Prod (PP), followed by ascending sequence number.'],
      ['delimiter', 'An underscore \'_\' delimiter between each part.']]
  );

  constructor(private readonly fb: FormBuilder, private readonly http: HttpClient, protected session: SessionService, protected configuration: ConfigurationService) {
    const defaultValidDate = new Date();
    defaultValidDate.setFullYear(defaultValidDate.getFullYear() + 4);

    this.generationForm = this.fb.group({
      components: this.fb.group({}),
      validUntil: [defaultValidDate.toISOString().substring(0, 10), [Validators.required]],
      country: ['', [Validators.required]],
      state: ['', [Validators.required]],
      locality: ['', [Validators.required]],
      organisation: ['', [Validators.required, OrganisationValidator.validate()]],
      platform: ['', [Validators.pattern(/^[a-z]*$/)]],
    });

    effect(() => {
      this.generationForm.setControl(
        'components',
        this.fb.group(
          this.configuration.availableComponentSignal().reduce(
            (control, value) => ({...control, [value.key]: [true]}),
            {}
          ),
          {validators: requireCheckboxesToBeCheckedValidator()}
        )
      );
    });

    effect(() => {
      const defaultValue = this.configuration.certificateDefaultValueSignal();
      if (defaultValue) {
        this.generationForm.patchValue({
          country: defaultValue.country,
          state: defaultValue.state,
          locality: defaultValue.locality,
          organisation: defaultValue.organisation
        });
      }
    });
  }

  get organisation() {
    return this.generationForm.get('organisation');
  }

  generateKeystores() {
    const formComponents: FormGroup = this.generationForm.get('components') as FormGroup;
    const selectedComponents: string[] = Object.entries(formComponents.getRawValue())
      .filter((kv) => kv[1] === true)
      .map((kv) => kv[0]);

    const dto = <KeystoreProperties>{
      validUntil: this.generationForm.get('validUntil')?.value,
      country: this.generationForm.get('country')?.value,
      state: this.generationForm.get('state')?.value,
      locality: this.generationForm.get('locality')?.value,
      organisation: this.generationForm.get('organisation')?.value,
      wantedComponents: selectedComponents,
      platform: this.generationForm.get('platform')?.value,
    };

    this.session.getSession().pipe(
      switchMap(sessionId => this.http.post(`${API_BASE_PATH}/key-store-generation/${sessionId}`, dto)),
    ).subscribe(() => this.session.update());

    this.generationInProgress = true;
  }

  hasError(policy: string): boolean {
    if (this.organisation?.value === '') {
      return true;
    }

    const errors = this.organisation?.errors;
    if (!errors?.['organisation']) return false;

    return errors['organisation'][policy];
  }
}

function requireCheckboxesToBeCheckedValidator(minRequired = 1): ValidatorFn {
  return function validate(controlGroup: AbstractControl): ValidationErrors | null {
    const formGroup = <FormGroup>controlGroup;
    let checked = 0;

    for (const key of Object.keys(formGroup.controls)) {
      if (formGroup.controls[key].value) {
        checked++
      }
    }

    if (checked >= minRequired) {
      return null;
    } else {
      return {notEnough: true}
    }
  };
}

interface KeystoreProperties {
  validUntil: string
  country: string;
  state: string;
  locality: string;
  organisation: string;
  wantedComponents: string[];
}
