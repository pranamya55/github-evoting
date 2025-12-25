/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import { TranslatableText } from 'e-voting-libraries-ui-kit';
import { ExtendedFactor } from './extended-factor';
import { HeaderConfig } from './header-config';

export interface VoterPortalConfig {
	electionEventId: string;
	identification: ExtendedFactor;
	contestsCapabilities: ContestsCapabilities;
	requestTimeout: RequestTimeout;
	header?: HeaderConfig;
	translatePlaceholders?: TranslatePlaceholders;
	additionalLegalTerms?: AdditionalLegalTerms;
	additionalFAQs?: AdditionalFAQs;
	favicon?: string;
}

// Contests capabilities
export interface ContestsCapabilities {
	writeIns: boolean;
}

// Request timeout
export interface RequestTimeout {
	authenticateVoter: number;
	sendVote: number;
	confirmVote: number;
}

// Translate placeholders
export type TranslatePlaceholders = Record<string, TranslatableText>;

// Additional legal terms
export interface AdditionalLegalTerms {
	mainTitle: TranslatableText;
	sections: LegalTermSection[];
}

export interface LegalTermSection {
	sectionTitle: TranslatableText;
	terms: TranslatableText[];
	confirm: TranslatableText;
}

// Additional FAQs
export interface FAQSectionContent {
	faqTitle: TranslatableText;
	content: TranslatableText[];
}

export type AdditionalFAQs = FAQSectionContent[];
