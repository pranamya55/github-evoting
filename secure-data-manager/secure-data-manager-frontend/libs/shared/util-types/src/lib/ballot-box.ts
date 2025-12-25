/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {BallotBoxCategory} from "./ballot-box-category";

export interface BallotBox {
	id: string;
	description: string;
	category: BallotBoxCategory;
	test: boolean;
	startTime: string;
	finishTime: string;
	gracePeriod: number;
}
