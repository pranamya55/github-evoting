/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

// Header config
export interface HeaderConfig {
	logo: string;
	logoHeight: ResponsiveConfig<number>;
	reverse?: boolean;
	background?: string;
	bars?: HeaderBarConfig[];
}

interface HeaderBarConfig {
	color: string;
	height: ResponsiveConfig<number>;
}

export interface ResponsiveConfig<T = unknown> {
	mobile: T;
	desktop: T;
}
