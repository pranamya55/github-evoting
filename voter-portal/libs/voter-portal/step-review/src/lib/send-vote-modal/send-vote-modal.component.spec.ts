/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { MockStore } from '@ngrx/store/testing';
import { MockProvider } from 'ng-mocks';
import { TranslateTestingModule } from 'ngx-translate-testing';
import { MockStoreProvider, setState } from '@vp/shared-util-testing';
import { BackendError } from '@vp/voter-portal-util-types';

import { SendVoteModalComponent } from './send-vote-modal.component';

describe('SendVoteModalComponent', () => {
	let fixture: ComponentFixture<SendVoteModalComponent>;
	let store: MockStore;
	let activeModal: NgbActiveModal;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			declarations: [SendVoteModalComponent],
			imports: [TranslateTestingModule.withTranslations({})],
			providers: [MockProvider(NgbActiveModal), MockStoreProvider()],
		}).compileComponents();
	});

	beforeEach(() => {
		fixture = TestBed.createComponent(SendVoteModalComponent);
		store = TestBed.inject(MockStore);
		activeModal = TestBed.inject(NgbActiveModal);

		activeModal.close = jest.fn();

		fixture.detectChanges();
	});

	it('should close when short choice return codes are registered in the store', () => {
		setState(store, {
			shortChoiceReturnCodes: [
				{
					questionIdentification: 'mockQuestionIdentification',
					shortChoiceReturnCode: 'mockChoiceReturnCode',
				},
			],
		});

		fixture.detectChanges();

		expect(activeModal.close).toHaveBeenCalledTimes(1);
	});

	it('should close when an error is registered in the store', () => {
		setState(store, { error: new Error('mockError') as BackendError });

		fixture.detectChanges();

		expect(activeModal.close).toHaveBeenCalledTimes(1);
	});
});
