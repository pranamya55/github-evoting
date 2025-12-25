/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ElectionEventStatus} from './election-event-status';

export interface ElectionEvent {
	id: string;
	defaultTitle: string;
	defaultDescription: string;
	alias: string;
	dateFrom: string;
	dateTo: string;
	gracePeriod: string;
	status: ElectionEventStatus;
	settings: { electionEvent: { id: string } };
	details: string;
	synchronized: string;
}
