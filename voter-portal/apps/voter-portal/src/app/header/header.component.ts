/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Component, inject} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Store} from '@ngrx/store';
import {TranslateService} from '@ngx-translate/core';
import {environment} from '@vp/voter-portal-data-access';
import {FAQService} from '@vp/voter-portal-feature-faq';
import {ConfigurationService} from '@vp/voter-portal-ui-services';
import {getCurrentLanguage, LanguageSelectorActions} from '@vp/voter-portal-ui-state';
import {Observable} from 'rxjs';
import {map, tap} from 'rxjs/operators';
import {FaviconService} from "@vp/shared-ui-services";

@Component({
	selector: 'vp-header',
	templateUrl: './header.component.html',
	standalone: false
})
export class HeaderComponent {
	readonly configuration = inject(ConfigurationService);
	private readonly store = inject(Store);
	private readonly translate = inject(TranslateService);
	private readonly faqService = inject(FAQService);
	private readonly route = inject(ActivatedRoute);
	private readonly router = inject(Router);
	private readonly faviconService = inject(FaviconService);

	availableLanguages = environment.availableLanguages;
	currentLang$: Observable<string>;
	skipLink$: Observable<string>;
	isCollapsed = true;

	constructor() {
		const translate = this.translate;

		this.currentLang$ = this.store.select(getCurrentLanguage).pipe(
			map((currentLanguage) => currentLanguage && currentLanguage.trim() !== "" ? currentLanguage : translate.defaultLang),
			tap((lang) => translate.reloadLang(lang)),
			tap((lang) => translate.use(lang)),
			tap((lang) => (document.documentElement.lang = lang))
		);

		if (this.configuration.favicon) {
			this.faviconService.setFavicon(this.configuration.favicon);
		}

		this.skipLink$ = this.route.fragment.pipe(
			map((fragment) =>
				this.router.url.replace(new RegExp(`#${fragment}$`), '')
			)
		);
	}

	translateApp(lang: string) {
		this.store.dispatch(LanguageSelectorActions.languageSelected({lang}));
	}

	showFAQ(): void {
		this.faqService.showFAQ();
	}
}
