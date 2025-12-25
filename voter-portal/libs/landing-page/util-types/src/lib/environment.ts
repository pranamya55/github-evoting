/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

export interface Environment {
	production: boolean;
	captcha?: CaptchaConfig
}

interface CaptchaConfig {
	url: string;
	responseParameterName: string;
	siteKey: string;
}