/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Title} from "@angular/platform-browser";
import {AsyncPipe} from "@angular/common";
import {TranslateService} from "@ngx-translate/core";
import {Component, inject} from '@angular/core';
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";
import {ContentElementComponent, ModeElementComponent} from "@vp/landing-page-ui-components";
import {combineLatest, Observable} from "rxjs";
import {TenantConfigurationService} from "@vp/landing-page-data-access";
import {ContentElement, HeadingContentElement} from "@vp/landing-page-utils-types";

@Component({
	selector: 'vp-content',
	templateUrl: './content-page.component.html',
	imports: [AsyncPipe, ContentElementComponent, ModeElementComponent]
})
export class ContentPageComponent {
	private readonly translate = inject(TranslateService);
	private readonly titleService = inject(Title);
	private readonly configurationService: TenantConfigurationService = inject(TenantConfigurationService);

	contentConfiguration!: Observable<ContentElement[]>;

	constructor() {
		this.contentConfiguration = this.configurationService.getContentConfiguration();

		combineLatest([this.translate.onLangChange, this.contentConfiguration])
			.pipe(
				takeUntilDestroyed(),
			)
			.subscribe(([{lang, translations}, configuration]) => {
				let pageTitle = translations['common.pageTitle'];

				const mainHeadingElement = configuration.find((el): el is { heading: HeadingContentElement } => 'heading' in el && el.heading.level === 1);
				const mainHeadingText = mainHeadingElement?.heading.text;
				if (mainHeadingText && lang.toUpperCase() in mainHeadingText) {
					pageTitle += ` - ${mainHeadingText[lang.toUpperCase() as keyof typeof mainHeadingText]}`;
				}

				this.titleService.setTitle(pageTitle);
			});
	}
}
