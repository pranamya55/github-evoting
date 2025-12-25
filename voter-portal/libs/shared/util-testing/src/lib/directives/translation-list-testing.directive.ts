/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {
	Directive,
	Input,
	TemplateRef,
	ViewContainerRef,
	inject,
} from '@angular/core';

@Directive({
	selector: '[vpTranslationList]',
	standalone: false,
})
export class TranslationListTestingDirective {
	private readonly templateRef = inject<
		TemplateRef<{
			key: string;
		}>
	>(TemplateRef);
	private readonly viewContainer = inject(ViewContainerRef);

	@Input() set vpTranslationList(key: string | undefined) {
		if (key) {
			this.viewContainer.createEmbeddedView(this.templateRef, { key });
		}
	}
}
