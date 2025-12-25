/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { AfterViewInit, Component, inject, Input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { TranslateService } from '@ngx-translate/core';
import { ConfigurationService } from '@vp/voter-portal-ui-services';
import { ExtendedFactor, FAQSection, FAQSectionContent } from '@vp/voter-portal-util-types';
import { Observable } from 'rxjs';

@Component({
	selector: 'vp-faq',
	templateUrl: './faq-modal.component.html',
	standalone: false,
})
export class FAQModalComponent implements AfterViewInit {
	readonly configuration = inject(ConfigurationService);
	readonly activeModal = inject(NgbActiveModal);
	private readonly translate = inject(TranslateService);

	additionalFAQs$!: Observable<FAQSectionContent[]>;
	@Input() activeFAQSection: FAQSection | undefined;

	readonly FAQSection = FAQSection;
	readonly ExtendedFactor = ExtendedFactor;
	readonly allowedCharacters =
		"ÀÁÂÃÄÅ àáâãäå ÈÉÊË èéêë ÌÍÎÏ ìíîï ÒÓÔÕÖ òóôõö ÙÚÛÜ ùúûü Ææ Çç Œœ Þþ Ññ Øø Šš Ýý Ÿÿ Žž ¢ () ð Ð ß ' , - . /";

	ngAfterViewInit() {
		if (this.activeFAQSection) {
			const activeSectionButton = document.querySelector<HTMLButtonElement>(
				`#${this.activeFAQSection}-header > .accordion-button`,
			);
			activeSectionButton?.focus();
		}
	}
}
