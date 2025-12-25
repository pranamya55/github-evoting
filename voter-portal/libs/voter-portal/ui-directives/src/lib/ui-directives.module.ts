/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ConditionallyDescribedByDirective } from './conditionally-described-by.directive';
import { MultilineInputDirective } from './multiline-input.directive';
import { TranslationListDirective } from './translation-list.directive';

@NgModule({
	imports: [CommonModule],
	declarations: [
		TranslationListDirective,
		ConditionallyDescribedByDirective,
		MultilineInputDirective,
	],
	exports: [
		TranslationListDirective,
		ConditionallyDescribedByDirective,
		MultilineInputDirective,
	],
})
export class UiDirectivesModule {}
