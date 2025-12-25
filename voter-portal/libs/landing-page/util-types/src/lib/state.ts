/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

export interface TenantState {
	tenant: string;
	activeMode?: string;
	events: TenantStateEvent[];
}

export interface TenantStateEvent {
	id: string;
	activePhase: string;
}