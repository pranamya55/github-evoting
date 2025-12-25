/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Injectable, inject } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { FAQSection } from '@vp/voter-portal-util-types';
import { FAQModalComponent } from './faq-modal/faq-modal.component';

@Injectable({
	providedIn: 'root',
})
export class FAQService {
	private readonly modalService = inject(NgbModal);

	public showFAQ(section?: FAQSection) {
		const modalOption = { fullscreen: 'xl', size: 'xl' };
		const modalRef = this.modalService.open(FAQModalComponent, modalOption);
		modalRef.componentInstance.activeFAQSection = section;
	}
}
