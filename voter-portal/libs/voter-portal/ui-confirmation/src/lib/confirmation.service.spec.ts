/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {fakeAsync, TestBed, tick} from '@angular/core/testing';
import {NgbModal, NgbModalRef} from '@ng-bootstrap/ng-bootstrap';
import {RandomBetween} from '@vp/shared-util-testing';
import {MockProvider} from 'ng-mocks';
import {ConfirmationModalComponent} from './confirmation-modal/confirmation-modal.component';

import {ConfirmationService} from './confirmation.service';

describe('ConfirmationService', () => {
	let service: ConfirmationService;
	let ngbModal: NgbModal;
	let modalRef: NgbModalRef;

	const defaultConfig = {content: 'Content'};

	beforeEach(() => {
		modalRef = {
			componentInstance: {},
			result: Promise.resolve(),
		} as NgbModalRef;

		TestBed.configureTestingModule({
			providers: [MockProvider(NgbModal, { open: () => modalRef })],
		});

		service = TestBed.inject(ConfirmationService);
		ngbModal = TestBed.inject(NgbModal);
	});

	describe('confirm', () => {
		it('should open a confirmation modal with the provided modal options', () => {
			const modalOptions = { size: 'xl' };
			const config = {
				...defaultConfig,
				modalOptions,
			};

			jest.spyOn(ngbModal, 'open');
			service.confirm(config);

			expect(ngbModal.open).toHaveBeenNthCalledWith(
				1,
				ConfirmationModalComponent,
				modalOptions,
			);
		});

		it('should assign the properties of the provided configuration to the modal instance', () => {
			const config = {
				...defaultConfig,
				title: 'Title',
				confirmLabel: 'Confirm',
				cancelLabel: 'Cancel',
			};

			service.confirm(config);

			Object.keys(config).forEach((configItem) => {
				expect(modalRef.componentInstance[configItem]).toBe(
					config[configItem as keyof typeof config],
				);
			});
		});

		it('should use the default title if none is provided in the configuration', () => {
			service.confirm(defaultConfig);

			expect(modalRef.componentInstance.title).toBe('common.confirmaction');
		});

		it('should use the default label for the confirm button if none is provided in the configuration', () => {
			service.confirm(defaultConfig);

			expect(modalRef.componentInstance.confirmLabel).toBe('common.confirm');
		});

		it('should use the default label for the cancel button if none is provided in the configuration', () => {
			service.confirm(defaultConfig);

			expect(modalRef.componentInstance.cancelLabel).toBe('common.cancel');
		});

		it('should use the default label for the cancel button if none is provided in the configuration', () => {
			service.confirm(defaultConfig);

			expect(modalRef.componentInstance.cancelLabel).toBe('common.cancel');
		});

		describe('confirmation result', () => {
			let confirmationEmitted: boolean;
			let errorThrown: boolean;
			let observableCompleted: boolean;

			const modalResult = { closed: true, dismissed: false };

			beforeEach(() => {
				confirmationEmitted = errorThrown = observableCompleted = false;
			});

			function setModalResult(modalResolved: boolean) {
				modalRef.result = modalResolved
					? Promise.resolve(RandomBetween(true, false))
					: Promise.reject();

				const confirmationResult = service.confirm(defaultConfig);

				confirmationResult.subscribe(
					() => {
						confirmationEmitted = true;
					},
					() => {
						errorThrown = true;
					},
					() => {
						observableCompleted = true;
					},
				);
			}

			it('should return an observable that emits before completing if the modal is closed', fakeAsync(() => {
				setModalResult(modalResult.closed);

				tick();

				expect(confirmationEmitted).toBeTruthy();
				expect(errorThrown).toBeFalsy();
				expect(observableCompleted).toBeTruthy();
			}));

			it('should return an observable that completes without emitting if the modal is dismissed', fakeAsync(() => {
				setModalResult(modalResult.dismissed);

				tick();

				expect(confirmationEmitted).toBeFalsy();
				expect(errorThrown).toBeFalsy();
				expect(observableCompleted).toBeTruthy();
			}));
		});
	});
});
