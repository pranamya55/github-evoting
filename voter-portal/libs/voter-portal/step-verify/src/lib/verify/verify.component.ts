/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, inject, Signal } from '@angular/core';
import { Store } from '@ngrx/store';
import {CancelMode, FAQSection} from '@vp/voter-portal-util-types';
import {
	ElectionTexts,
	isElectionTexts,
	isVoteTexts,
	VoteTexts,
} from 'e-voting-libraries-ui-kit';
import {
	getElectionsTexts,
	getLoading,
	getVoteSentButNotCastInPreviousSession,
	getVotesTexts,
} from '@vp/voter-portal-ui-state';
import { FAQService } from '@vp/voter-portal-feature-faq';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ModalInvalidCodesComponent } from '@vp/voter-portal-ui-components';

@Component({
	selector: 'vp-verify',
	templateUrl: './verify.component.html',
	standalone: false,
})
export class VerifyComponent {
	private readonly store = inject(Store);
	private readonly faqService = inject(FAQService);

	protected readonly CancelMode = CancelMode;
	protected readonly FAQSection = FAQSection;
	protected readonly isVoteTexts = isVoteTexts;
	protected readonly isElectionTexts = isElectionTexts;

	private readonly modalService = inject(NgbModal);

	votesTexts: Signal<VoteTexts[]> = this.store.selectSignal(getVotesTexts);
	electionsTexts: Signal<ElectionTexts[]> =
		this.store.selectSignal(getElectionsTexts);
	isLoading: Signal<boolean> = this.store.selectSignal(getLoading);
	voteSentButNotCastInPreviousSession: Signal<boolean> = this.store.selectSignal(getVoteSentButNotCastInPreviousSession);

	openInvalidCodesModal() {
		const modalRef = this.modalService.open(ModalInvalidCodesComponent, {
			size: 'lg',
			backdrop: "static",
			keyboard: false,
		});
		Object.assign(modalRef.componentInstance, {
			title: 'verify.codesdonotmatch.title',
			message: 'verify.codesdonotmatch.message',
			cancelMode: CancelMode.LeaveProcess,
		});
	}

	showFAQ(section: FAQSection): void {
		this.faqService.showFAQ(section);
	}
}
