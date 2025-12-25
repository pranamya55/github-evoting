/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalInvalidCodesComponent } from './modal-invalid-codes.component';
import { FAQService } from '@vp/voter-portal-feature-faq';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { MockComponent, MockDirective, MockPipe, MockProvider } from 'ng-mocks';
import { TranslatePipe } from '@ngx-translate/core';
import { IconComponent } from '@vp/shared-ui-components';
import { ProcessCancellationService } from "@vp/voter-portal-ui-services";
import { TranslationListDirective } from "@vp/voter-portal-ui-directives";

describe('ModalInvalidCodesComponent', () => {
	let component: ModalInvalidCodesComponent;
	let fixture: ComponentFixture<ModalInvalidCodesComponent>;

	beforeEach(async () => {
		await TestBed.configureTestingModule({
			imports: [MockComponent(IconComponent)],
			declarations: [
				ModalInvalidCodesComponent,
				MockPipe(TranslatePipe),
				MockDirective(TranslationListDirective),
			],
			providers: [
				MockProvider(NgbActiveModal, { dismiss: jest.fn() }),
				MockProvider(FAQService, { showFAQ: jest.fn() }),
				MockProvider(ProcessCancellationService, { cancelVote: jest.fn() }),
			],
		}).compileComponents();

		fixture = TestBed.createComponent(ModalInvalidCodesComponent);
		component = fixture.componentInstance;
		fixture.detectChanges();
	});

	it('should create the component', () => {
		expect(component).toBeTruthy();
	});
});
