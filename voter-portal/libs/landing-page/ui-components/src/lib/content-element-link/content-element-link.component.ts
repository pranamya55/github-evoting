/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {NgClass} from "@angular/common";
import {IconComponent} from "@vp/shared-ui-components";
import {TranslateTextPipe} from "e-voting-libraries-ui-kit";
import {ButtonItemContentElement} from "@vp/landing-page-utils-types";
import {Component, inject, Input, OnInit} from '@angular/core';
import {LangChangeEvent, TranslateService} from "@ngx-translate/core";

@Component({
	selector: 'vp-content-element-link',
	imports: [
		NgClass,
		IconComponent,
		TranslateTextPipe
	],
	templateUrl: './content-element-link.component.html',
})
export class ContentElementLinkComponent implements OnInit {
	@Input({required: true}) element!: ButtonItemContentElement;
	@Input({required: false}) primary: boolean = false;
	@Input({required: false}) secondary: boolean = false;
	@Input({required: false}) block: boolean = false;

	private readonly translateService = inject(TranslateService);
	currentLanguage!: string;

	ngOnInit(): void {
		if (!this.element.addLanguageParameter) {
			return;
		}

		if (this.translateService.currentLang === 'en') {
			this.currentLanguage = this.translateService.getDefaultLang();
		} else {
			this.currentLanguage = this.translateService.currentLang || this.translateService.getDefaultLang();
		}

		this.translateService.onLangChange.subscribe((event: LangChangeEvent) => {
			this.currentLanguage = event.lang !== 'en' ? event.lang : this.translateService.getDefaultLang();
		});

	}
}
