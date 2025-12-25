/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { RandomArray } from '@vp/shared-util-testing';
import {MockDirective, MockProvider} from 'ng-mocks';
import { TranslateTestingModule } from 'ngx-translate-testing';
import {TranslationListDirective} from "@vp/voter-portal-ui-directives";

import { ConfirmationModalComponent } from './confirmation-modal.component';

describe('ConfirmationModalComponent', () => {
	let component: ConfirmationModalComponent;
	let fixture: ComponentFixture<ConfirmationModalComponent>;
	let dismiss: jest.Mock<() => void>;
	let close: jest.Mock<() => void>;

	beforeEach(async () => {
		dismiss = jest.fn();
		close = jest.fn();

		await TestBed.configureTestingModule({
			imports: [TranslateTestingModule.withTranslations({})],
			declarations: [ConfirmationModalComponent, MockDirective(TranslationListDirective)],
			providers: [MockProvider(NgbActiveModal, { dismiss, close })],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(ConfirmationModalComponent);
		component = fixture.componentInstance;
	});

	describe('content', () => {
		let modalBody: HTMLElement;

		function setContent(content: string | string[]) {
			component.content = content;

			fixture.detectChanges();

			modalBody = fixture.debugElement.query(
				By.css('.modal-body'),
			).nativeElement;
		}

		it('should throw an error if no content is provided', () => {
			expect(() => fixture.detectChanges()).toThrow();
		});
	});

	describe('title', () => {
		let modalTitle: HTMLElement;

		beforeEach(() => {
			component.content = 'Content';

			fixture.detectChanges();

			modalTitle = fixture.debugElement.query(
				By.css('.modal-title'),
			).nativeElement;
		});

		it('should properly show provided title', () => {
			const title = (component.title = 'Title');

			fixture.detectChanges();

			expect(modalTitle.textContent).toBe(title);
		});
	});

	describe('buttons', () => {
		let confirmButton: HTMLButtonElement;
		let cancelButton: HTMLButtonElement;
		let closeButton: HTMLButtonElement;

		beforeEach(() => {
			component.content = 'Content';

			fixture.detectChanges();

			const modalButtons = fixture.debugElement.queryAll(
				By.css('.modal-footer > button'),
			);
			confirmButton = modalButtons[0].nativeElement;
			cancelButton = modalButtons[1].nativeElement;
			closeButton = fixture.debugElement.query(
				By.css('.btn-close'),
			).nativeElement;
		});

		it('should properly show provided confirm label', () => {
			const confirmLabel = (component.confirmLabel = 'Confirm');

			fixture.detectChanges();

			expect(confirmButton.textContent.trim()).toBe(confirmLabel);
		});

		it('should properly show provided cancel label', () => {
			const cancelLabel = (component.cancelLabel = 'Cancel');

			fixture.detectChanges();

			expect(cancelButton.textContent).toBe(cancelLabel);
		});

		it('should call the "close" method with true when the confirm button is clicked', () => {
			confirmButton.click();

			expect(close).toHaveBeenNthCalledWith(1, true);
			expect(dismiss).not.toHaveBeenCalled();
		});

		it('should call the "close" method with false when the cancel button is clicked', () => {
			cancelButton.click();

			expect(close).toHaveBeenNthCalledWith(1, false);
			expect(dismiss).not.toHaveBeenCalled();
		});

		it('should call the "dismiss" method when the close button is clicked', () => {
			closeButton.click();

			expect(close).not.toHaveBeenCalled();
			expect(dismiss).toHaveBeenCalledTimes(1);
		});
	});
});
