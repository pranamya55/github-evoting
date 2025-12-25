/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

import { LegalTermsComponent } from './legal-terms/legal-terms.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { UiComponentsModule } from '@vp/voter-portal-ui-components';
import { MarkdownPipe, TranslateTextPipe } from 'e-voting-libraries-ui-kit';
import {IconComponent} from "@vp/shared-ui-components";

@NgModule({
	imports: [
		CommonModule,
		TranslateModule,
		FormsModule,
		ReactiveFormsModule,
		UiComponentsModule,
		MarkdownPipe,
		TranslateTextPipe,
		IconComponent,
	],
	declarations: [LegalTermsComponent],
	exports: [LegalTermsComponent],
})
export class LegalTermsModule {}
