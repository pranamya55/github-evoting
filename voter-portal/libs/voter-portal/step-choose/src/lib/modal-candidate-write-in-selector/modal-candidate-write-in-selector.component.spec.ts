/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalCandidateWriteInSelectorComponent } from './modal-candidate-write-in-selector.component';
import { MockComponent, MockModule, MockPipe, MockProvider } from 'ng-mocks';
import { TranslatePipe } from '@ngx-translate/core';
import { SortByPipe, TranslateTextPipe } from 'e-voting-libraries-ui-kit';
import { mockSortBy, mockTranslateText } from '@vp/shared-util-testing';
import { NgbActiveModal, NgbCollapseModule } from '@ng-bootstrap/ng-bootstrap';
import { IconComponent } from '@vp/shared-ui-components';
import { By } from '@angular/platform-browser';

describe('ModalCandidateWriteInSelectorComponent', () => {
	let component: ModalCandidateWriteInSelectorComponent;
	let fixture: ComponentFixture<ModalCandidateWriteInSelectorComponent>;
	let modalClose: jest.Mock;

	beforeEach(async () => {
		modalClose = jest.fn();

		await TestBed.configureTestingModule({
			imports: [MockModule(NgbCollapseModule), MockComponent(IconComponent)],
			declarations: [
				ModalCandidateWriteInSelectorComponent,
				MockPipe(TranslatePipe, (value) => value),
				MockPipe(TranslateTextPipe, mockTranslateText),
				MockPipe(SortByPipe, mockSortBy),
			],
			providers: [MockProvider(NgbActiveModal, { close: modalClose })],
		}).compileComponents();

		fixture = TestBed.createComponent(ModalCandidateWriteInSelectorComponent);
		component = fixture.componentInstance;

		fixture.detectChanges();
	});

	function getSelectListButton() {
		return fixture.debugElement.query(By.css('button'));
	}

	it('should create', () => {
		expect(component).toBeTruthy();
	});

	it('should display the "Select" button when write-in-position is not already selected', () => {
		expect(
			getSelectListButton()
				.query(By.directive(IconComponent))
				.nativeElement.getAttribute('name'),
		).toBe('check2');
		expect(
			getSelectListButton()
				.query(By.css('span[aria-hidden="true"]'))
				.nativeElement.textContent.trim(),
		).toBe('candidateselection.candidateresult.select');
	});

	it('should call selectWriteIn() and close modal with no params when button is clicked', () => {
		getSelectListButton().nativeElement.click();
		expect(modalClose).toHaveBeenNthCalledWith(1);
	});
});
