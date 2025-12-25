/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NgbTooltip } from '@ng-bootstrap/ng-bootstrap';
import { MockResizeObserver } from '@vp/shared-util-testing';
import { MockComponent, MockDirective } from 'ng-mocks';
import { TranslateTestingModule } from 'ngx-translate-testing';

import { ClearableInputComponent } from './clearable-input.component';
import {IconComponent} from "@vp/shared-ui-components";

@Component({
	template: `
		<vp-clearable-input>
			<input class="form-control" [formControl]="formControl" />
		</vp-clearable-input>
	`,
	standalone: false,
})
class TestHostComponent {
	formControl = new FormControl('value');
}

describe('ClearableInputComponent', () => {
	window.ResizeObserver = MockResizeObserver;

	let testHost: TestHostComponent;
	let fixture: ComponentFixture<TestHostComponent>;
	let formControl: DebugElement;
	let clearButton: DebugElement;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ClearableInputComponent,
				TestHostComponent,
				MockDirective(NgbTooltip),
			],
			imports: [
				MockComponent(IconComponent),
				ReactiveFormsModule,
				TranslateTestingModule.withTranslations({}),
			],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(TestHostComponent);
		testHost = fixture.componentInstance;

		fixture.detectChanges();

		formControl = fixture.debugElement.query(By.css('.form-control'));
	});

	function setValue(value: string) {
		testHost.formControl.setValue(value);

		return fixture.whenStable().then(() => {
			fixture.detectChanges();
			clearButton = fixture.debugElement.query(By.css('.btn-clear'));
		});
	}

	describe('with an empty form control', () => {
		beforeEach(async () => {
			await setValue('');
		});

		it('should show an input', function () {
			expect(formControl).toBeTruthy();
		});

		it('should not show a clear button', function () {
			expect(clearButton.nativeElement.getAttribute('hidden')).not.toBeNull();
		});
	});

	describe('with a filled-in form control', () => {
		beforeEach(async () => {
			await setValue('Non empty value');
		});

		it('should show an input', function () {
			expect(formControl).toBeTruthy();
		});

		it('should show a clear button', function () {
			expect(clearButton.nativeElement.getAttribute('hidden')).toBeNull();
		});

		it('should clear the form control value in the view after clicking the clear button', function () {
			clearButton.nativeElement.click();

			fixture.detectChanges();

			expect(formControl.nativeElement.value).toBe('');
		});

		it('should clear the form control value in the model after clicking the clear button', function () {
			clearButton.nativeElement.click();

			fixture.detectChanges();

			expect(testHost.formControl.value).toBe('');
		});
	});
});
