/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { TranslationListTestingDirective } from './directives/translation-list-testing.directive';

@NgModule({
	declarations: [TranslationListTestingDirective],
	imports: [CommonModule],
	exports: [TranslationListTestingDirective],
})
export class SharedTestingModule {}
