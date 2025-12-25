/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {FooterElement} from "./footer-element";
import {ContentElement} from "./content-element";
import {HeaderConfiguration} from "./header-configuration";
import {LanguageConfiguration} from "./language-configuration";

export interface PageConfiguration {
	headerConfiguration: HeaderConfiguration;
	contentConfiguration: ContentElement[];
	footerConfiguration: FooterElement;
	languageConfiguration?: LanguageConfiguration;
}