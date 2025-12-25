/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalCandidateSelectorEnabledComponent } from './modal-candidate-selector-enabled.component';
import { MockComponent, MockPipe, MockProvider } from 'ng-mocks';
import { TranslatePipe } from '@ngx-translate/core';
import { TranslateTextPipe } from 'e-voting-libraries-ui-kit';
import { MockCandidate } from '@vp/shared-util-testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { IconComponent } from '@vp/shared-ui-components';
import { By } from '@angular/platform-browser';

describe('ModalCandidateSelectorEnabledComponent', () => {
	let component: ModalCandidateSelectorEnabledComponent;
	let fixture: ComponentFixture<ModalCandidateSelectorEnabledComponent>;
	let modalClose: jest.Mock;

	beforeEach(async () => {
		modalClose = jest.fn();

		await TestBed.configureTestingModule({
			declarations: [
				ModalCandidateSelectorEnabledComponent,
				MockPipe(TranslatePipe, (value) => value),
				MockPipe(TranslateTextPipe),
			],
			imports: [MockComponent(IconComponent)],
			providers: [MockProvider(NgbActiveModal, { close: modalClose })],
		}).compileComponents();

		fixture = TestBed.createComponent(ModalCandidateSelectorEnabledComponent);
		component = fixture.componentInstance;

		component.candidate = MockCandidate();

		fixture.detectChanges();
	});

	function getButton() {
		return fixture.debugElement.query(By.css('button'));
	}

	function getButtonIcon() {
		return getButton().query(By.directive(IconComponent));
	}

	function getButtonLabel() {
		return getButton().query(By.css('span[aria-hidden="true"]'));
	}

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should display the "Accumulate" button when candidate is already selected', () => {
		component.isAlreadySelected = true;

		fixture.detectChanges();

		expect(getButtonIcon().nativeElement.getAttribute('name')).toBe('plus-lg');
		expect(getButtonLabel().nativeElement.textContent.trim()).toBe(
			'candidateselection.candidateresult.accumulate',
		);
	});

	it('should display the "Select" button when candidate is not already selected', () => {
		component.isAlreadySelected = false;

		fixture.detectChanges();

		expect(getButtonIcon().nativeElement.getAttribute('name')).toBe('check2');
		expect(getButtonLabel().nativeElement.textContent.trim()).toBe(
			'candidateselection.candidateresult.select',
		);
	});

	it('should call selectCandidate() and close modal with candidate when button is clicked', () => {
		getButton().nativeElement.click();
		expect(modalClose).toHaveBeenNthCalledWith(1, component.candidate);
	});
});
