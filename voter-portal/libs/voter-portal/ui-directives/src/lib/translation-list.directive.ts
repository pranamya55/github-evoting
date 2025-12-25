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
import { TranslateService } from '@ngx-translate/core';
import { merge } from 'rxjs';
import { filter, take, tap } from 'rxjs/operators';

function translationExists(translationObject: object): boolean {
	return Object.entries(translationObject).every(
		([translationKey, translation]) => {
			return !!translation && translation !== translationKey;
		},
	);
}

@Directive({
	selector: '[vpTranslationList]',
	standalone: false,
})
export class TranslationListDirective {
	private readonly templateRef = inject<
		TemplateRef<{
			key: string;
		}>
	>(TemplateRef);
	private readonly viewContainer = inject(ViewContainerRef);
	private readonly translate = inject(TranslateService);

	translationKey: string | undefined;

	@Input() set vpTranslationList(translationKey: string | undefined) {
		if (!translationKey) {
			return;
		}

		this.translationKey = translationKey;

		const singleParagraph$ = this.translate.get([translationKey]).pipe(
			filter(translationExists),
			tap(() => this.showSingleParagraph()),
		);

		const multipleParagraphs$ = this.translate
			.get([`${translationKey}.1`])
			.pipe(
				filter(translationExists),
				tap(() => this.showMultipleParagraphs()),
			);

		merge(singleParagraph$, multipleParagraphs$).pipe(take(1)).subscribe();
	}

	private showSingleParagraph(): void {
		this.viewContainer.createEmbeddedView(this.templateRef, {
			key: this.translationKey,
		});
	}

	private showMultipleParagraphs(): void {
		let position = 1;
		let key = this.getkey(position);

		while (key && this.translationExistsForKey(key)) {
			this.viewContainer.createEmbeddedView(this.templateRef, { key });

			position++;
			key = this.getkey(position);
		}
	}

	private translationExistsForKey(key: string): boolean {
		const translation = this.translate.instant([key]);
		return translationExists(translation);
	}

	private getkey(position: number): string {
		return `${this.translationKey}.${position}`;
	}
}
