/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {VotingRoutePath} from "./voting-route-path";

export interface RouteData {
	reachablePaths?: VotingRoutePath[];
	navigateAwayAction?: NavigateAwayAction;
	headerOnly?: boolean;
	noHeader?: boolean;
}

export enum NavigateAwayAction {
	ShowCancelVoteDialog = 'ShowCancelDialog',
	ShowLeaveProcessDialog = 'ShowLeaveDialog',
	ShowQuitProcessDialog = 'ShowQuitDialog',
	GoToStartVotingPage = 'GoToStartVotingPage',
	GoToLegalTermsPage = 'GoToLegalTermsPage',
}
