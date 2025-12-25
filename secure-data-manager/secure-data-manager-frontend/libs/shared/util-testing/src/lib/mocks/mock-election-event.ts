/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {ElectionEvent, ElectionEventStatus} from '@sdm/shared-util-types';

let electionEventIndex = 0;

export class MockElectionEvent implements ElectionEvent {
	id = `electionEvent_${electionEventIndex}`;

	defaultTitle = `${this.id}_defaultTitle`;
	defaultDescription = `${this.id}_defaultDescription`;
	alias = `${this.id}_alias`;
	dateFrom = `${this.id}_dateFrom`;
	dateTo = `${this.id}_dateTo`;
	gracePeriod = `${this.id}_gracePeriod`;
	status = ElectionEventStatus.Locked;
	settings = {electionEvent: {id: `${this.id}_settings_electionEvent_id`}};
	details = `${this.id}_details`;
	synchronized = `${this.id}_synchronized`;

	constructor(props?: Partial<ElectionEvent>) {
		electionEventIndex++;
		Object.assign(this, props);
	}
}
