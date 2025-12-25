/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalCandidateSelectorDisabledComponent } from './modal-candidate-selector-disabled.component';
import { MockModule, MockPipe } from 'ng-mocks';
import { TranslatePipe } from '@ngx-translate/core';
import { UiDirectivesModule } from '@vp/voter-portal-ui-directives';
import { By } from '@angular/platform-browser';

describe('ModalCandidateSelectorDisabledComponent', () => {
	let component: ModalCandidateSelectorDisabledComponent;
	let fixture: ComponentFixture<ModalCandidateSelectorDisabledComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [
				ModalCandidateSelectorDisabledComponent,
				MockPipe(TranslatePipe, (value) => value),
			],
			imports: [MockModule(UiDirectivesModule)],
		}).compileComponents();

		fixture = TestBed.createComponent(ModalCandidateSelectorDisabledComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	function getButton() {
		return fixture.debugElement.query(By.css('button'));
	}

	function getMessage() {
		return fixture.debugElement.query(By.css('p'));
	}

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});

	it('should render a disabled button', () => {
		expect(getButton().nativeElement.disabled).toBe(true);
	});

	it('should display "not eligible" message when isEligible is false', () => {
		component.isEligible = false;

		fixture.detectChanges();

		expect(getMessage().nativeElement.textContent.trim()).toBe(
			'candidateselection.candidateresult.noteligible',
		);
	});

	it('should display "already selected on position" when isSelectedOnCurrentPosition is true', () => {
		component.isEligible = true;
		component.isSelectedOnCurrentPosition = true;

		fixture.detectChanges();

		expect(getMessage().nativeElement.textContent.trim()).toBe(
			'candidateselection.candidateresult.alreadyselectedonposition',
		);
	});

	it('should display "maximum accumulation reached" message when multiple accumulation is possible and maximum accumulation is reached', () => {
		component.isEligible = true;
		component.isSelectedOnCurrentPosition = false;
		component.hasReachedMaximumAccumulation = true;
		component.maximumAccumulation = 2;

		fixture.detectChanges();

		expect(getMessage().nativeElement.textContent.trim()).toBe(
			'candidateselection.candidateresult.maximumaccumulationreached',
		);
	});

	it('should display "already selected" message when only one accumulation is possible and maximum accumulation is reached', () => {
		component.isEligible = true;
		component.isSelectedOnCurrentPosition = false;
		component.hasReachedMaximumAccumulation = false;
		component.maximumAccumulation = 1;

		fixture.detectChanges();

		expect(getMessage().nativeElement.textContent.trim()).toBe(
			'candidateselection.candidateresult.alreadyselected',
		);
	});
});
