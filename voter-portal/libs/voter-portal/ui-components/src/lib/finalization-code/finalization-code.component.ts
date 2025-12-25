/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, inject,} from '@angular/core';
import {FAQSection} from "@vp/voter-portal-util-types";
import {FAQService} from "@vp/voter-portal-feature-faq";
import {getDefinedVoteCastReturnCode} from "@vp/voter-portal-ui-state";
import {map} from "rxjs/operators";
import {Store} from "@ngrx/store";

@Component({
	selector: 'vp-finalization-code',
	standalone: false,
	templateUrl: './finalization-code.component.html',
})
export class FinalizationCodeComponent {
	private readonly faqService = inject(FAQService);
	private readonly store = inject(Store);
	protected readonly FAQSection = FAQSection;

	voteCastReturnCode$ = this.store.pipe(
		getDefinedVoteCastReturnCode,
		map((voteCastReturnCode) => this.formatVoteCastCode(voteCastReturnCode)),
	);

	showFAQ(section: FAQSection): void {
		this.faqService.showFAQ(section);
	}

	formatVoteCastCode(voteCastReturnCode: string): string | null {
		return new RegExp(/^\d{8}$/).test(voteCastReturnCode)
			? `${voteCastReturnCode.slice(0, 4)} ${voteCastReturnCode.slice(4)}`
			: null;
	}
}
