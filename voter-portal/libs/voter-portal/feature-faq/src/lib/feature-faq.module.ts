/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { NgbAccordionModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';
import { FAQModalComponent } from './faq-modal/faq-modal.component';
import { UiDirectivesModule } from '@vp/voter-portal-ui-directives';
import { MarkdownPipe, TranslateTextPipe } from 'e-voting-libraries-ui-kit';

@NgModule({
	imports: [
		CommonModule,
		TranslateModule,
		NgbAccordionModule,
		UiDirectivesModule,
		MarkdownPipe,
		TranslateTextPipe,
	],
	declarations: [FAQModalComponent],
	exports: [FAQModalComponent],
})
export class FeatureFaqModule {}
