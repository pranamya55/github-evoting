/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
	FormControl,
	FormGroup,
	FormsModule,
	ReactiveFormsModule,
	Validators,
} from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NgbDatepickerModule } from '@ng-bootstrap/ng-bootstrap';
import {MockComponent, MockModule, MockProvider} from 'ng-mocks';
import { NgxMaskDirective, provideNgxMask } from 'ngx-mask';
import { TranslateTestingModule } from 'ngx-translate-testing';
import { ExtendedFactorComponent } from './extended-factor.component';
import { UiComponentsModule } from '@vp/voter-portal-ui-components';
import { ConfigurationService } from '@vp/voter-portal-ui-services';
import { ExtendedFactor } from '@vp/voter-portal-util-types';
import { RandomString } from '@vp/shared-util-testing';
import {IconComponent} from "@vp/shared-ui-components";

describe('ExtendedFactorComponent', () => {
	let component: ExtendedFactorComponent;
	let fixture: ComponentFixture<ExtendedFactorComponent>;
	let dateOfBirthInput: DebugElement;
	let yearOfBirthInput: DebugElement;

	function getContestFormGroup(): FormGroup {
		return new FormGroup({
			startVotingKey: new FormControl('', [Validators.required]),
			extendedFactor: new FormControl('', [Validators.required]),
		});
	}

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [
				FormsModule,
				ReactiveFormsModule,
				TranslateTestingModule.withTranslations('FR', {
					datepicker: { placeholder: 'dd.mm.yyyy' },
				}).withDefaultLanguage('FR'),
				MockModule(UiComponentsModule),
				MockModule(NgbDatepickerModule),
				NgxMaskDirective,
				MockComponent(IconComponent),
			],
			declarations: [ExtendedFactorComponent],
			providers: [MockProvider(ConfigurationService), provideNgxMask()],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(ExtendedFactorComponent);
		component = fixture.componentInstance;
		component.voterForm = getContestFormGroup();
		component.formSubmitted = false;
	});

	it('should not crash if there is no extended factor', () => {
		component.configuration.identification = '' as ExtendedFactor;
		fixture.detectChanges();
		const divs = fixture.debugElement.queryAll(By.css('div'));
		expect(divs.length).toBe(0);
	});

	describe('date of birth', () => {
		const getDobRequiredAlert = () => {
			return fixture.debugElement.query(By.css('#dateOfBirth-required'));
		};

		beforeEach(() => {
			component.configuration.identification = ExtendedFactor.DateOfBirth;
			fixture.detectChanges();
			dateOfBirthInput = fixture.debugElement.query(By.css('#dateOfBirth'));
			yearOfBirthInput = fixture.debugElement.query(By.css('#yearOfBirth'));
		});

		it('should show date of birth input', () => {
			expect(dateOfBirthInput).toBeTruthy();
		});

		it('should not show year of birth input', () => {
			expect(yearOfBirthInput).toBeFalsy();
		});

		it('should not show validation error "dateOfBirth-required" if form is not submitted yet', () => {
			expect(getDobRequiredAlert()).toBeFalsy();
		});

		it('should show validation error "dateOfBirth-required" if form is submitted and invalid', () => {
			component.formSubmitted = true;
			fixture.detectChanges();
			expect(getDobRequiredAlert()).toBeTruthy();
		});

		it('should not show validation error "dateOfBirth-required" if form is submitted and valid', () => {
			component.formSubmitted = true;
			component.extendedFactor.setValue('01011975');

			fixture.detectChanges();
			expect(getDobRequiredAlert()).toBeFalsy();
		});

		it('should format day-of-birth properly', () => {
			const dateOfBirthInputElem = fixture.debugElement.query(
				By.css('#dateOfBirth'),
			).nativeElement;
			dateOfBirthInputElem.value = '05.01.1975';
			dateOfBirthInputElem.dispatchEvent(new Event('input'));
			fixture.detectChanges();
			const formValue = component.extendedFactor.value;
			expect(formValue).toBe('05011975');
		});
	});

	describe('year of birth', () => {
		const getYobRequiredAlert = () => {
			return fixture.debugElement.query(By.css('#yearOfBirth-required'));
		};

		beforeEach(() => {
			component.configuration.identification = ExtendedFactor.YearOfBirth;
			fixture.detectChanges();
			dateOfBirthInput = fixture.debugElement.query(By.css('#dateOfBirth'));
			yearOfBirthInput = fixture.debugElement.query(By.css('#yearOfBirth'));
		});

		it('should show year of birth input', () => {
			expect(yearOfBirthInput).toBeTruthy();
		});

		it('should not show date of birth input', () => {
			expect(dateOfBirthInput).toBeFalsy();
		});

		it('should not show validation error "yearOfBirth-required" if form is not submitted yet', () => {
			expect(getYobRequiredAlert()).toBeFalsy();
		});

		it('should show validation error "yearOfBirth-required" if form is submitted and invalid', () => {
			component.formSubmitted = true;
			fixture.detectChanges();
			expect(getYobRequiredAlert()).toBeTruthy();
		});

		it('should not show validation error "yearOfBirth-required" if form is submitted and valid', () => {
			component.formSubmitted = true;
			component.extendedFactor.setValue('1975');
			fixture.detectChanges();
			expect(getYobRequiredAlert()).toBeFalsy();
		});

		it('should format year-of-birth properly', () => {
			const yearOfBirthInputElem = fixture.debugElement.query(
				By.css('#yearOfBirth'),
			).nativeElement;
			const randomYearOfBirth = RandomString(4, '0123456789');
			yearOfBirthInputElem.value = randomYearOfBirth;
			yearOfBirthInputElem.dispatchEvent(new Event('input'));
			fixture.detectChanges();
			const formValue = component.extendedFactor.value;
			expect(formValue).toBe(randomYearOfBirth);
		});
	});
});
