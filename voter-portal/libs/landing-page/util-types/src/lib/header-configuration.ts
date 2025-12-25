/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

export interface HeaderConfiguration {
	logo: HeaderLogoConfiguration;
	reverse?: boolean;
	background?: string;
	bars?: HeaderBarConfiguration[];
}
interface HeaderLogoConfiguration {
	title: Record<string, string>;
	height: Responsive<number>;
}
interface HeaderBarConfiguration {
	color: string;
	height: Responsive<number>;
}
interface Responsive<T> {
	mobile: T;
	desktop: T;
}
