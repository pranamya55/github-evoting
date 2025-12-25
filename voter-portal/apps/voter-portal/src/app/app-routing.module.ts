/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {NgModule} from '@angular/core';
import {PreloadAllModules, RouterModule, Routes} from '@angular/router';
import {PageNotFoundComponent} from './page-not-found/page-not-found.component';
import {FinalizationPageComponent} from "./finalization-page/finalization-page.component";
import {InitializationPageComponent} from "./intialization-page/initialization-page.component";
import {NavigateAwayAction, RouteData, VotingRoutePath} from "@vp/voter-portal-util-types";
import {areLegalTermsAccepted, confirmDeactivationIfNeeded, isConfigLoaded, isElectionEventIdValid} from "@vp/voter-portal-ui-guards";
import {ChooseComponent} from "@vp/voter-portal-step-choose";
import {ConfirmComponent} from "@vp/voter-portal-step-confirm";
import {LegalTermsComponent} from "@vp/voter-portal-step-legal-terms";
import {ReviewComponent} from "@vp/voter-portal-step-review";
import {StartVotingComponent} from "@vp/voter-portal-step-start-voting";
import {VerifyComponent} from "@vp/voter-portal-step-verify";
import {CastComponent} from "@vp/voter-portal-step-cast";

export const routes: Routes = [
	{
		path: '',
		pathMatch: 'full',
		component: PageNotFoundComponent,
		data: {
			headerOnly: true
		} as RouteData
	},
	{
		path: `${VotingRoutePath.Init}/:electionEventId`,
		data: {
			noHeader: true
		} as RouteData,
		canActivate: [isElectionEventIdValid],
		component: InitializationPageComponent,
	},
	{
		path: VotingRoutePath.LegalTerms,
		data: {
			reachablePaths: [VotingRoutePath.Init],
		} as RouteData,
		canActivate: [isConfigLoaded],
		canDeactivate: [confirmDeactivationIfNeeded],
		component: LegalTermsComponent,
	},
	{
		path: '',
		canActivateChild: [areLegalTermsAccepted],
		children: [
			{
				path: VotingRoutePath.StartVoting,
				data: {
					reachablePaths: [VotingRoutePath.Init, VotingRoutePath.LegalTerms]
				} as RouteData,
				canDeactivate: [confirmDeactivationIfNeeded],
				component: StartVotingComponent,
			},
			{
				path: VotingRoutePath.Choose,
				data: {
					navigateAwayAction: NavigateAwayAction.ShowCancelVoteDialog,
				} as RouteData,
				canDeactivate: [confirmDeactivationIfNeeded],
				component: ChooseComponent,
			},
			{
				path: VotingRoutePath.Review,
				data: {
					reachablePaths: [VotingRoutePath.Choose],
					navigateAwayAction: NavigateAwayAction.ShowCancelVoteDialog,
				} as RouteData,
				canDeactivate: [confirmDeactivationIfNeeded],
				component: ReviewComponent,
			},
			{
				path: VotingRoutePath.Verify,
				data: {
					navigateAwayAction: NavigateAwayAction.ShowLeaveProcessDialog,
				} as RouteData,
				canDeactivate: [confirmDeactivationIfNeeded],
				component: VerifyComponent,
			},
			{
				path: VotingRoutePath.Cast,
				data: {
					reachablePaths: [VotingRoutePath.Verify],
					navigateAwayAction: NavigateAwayAction.ShowLeaveProcessDialog,
				} as RouteData,
				canDeactivate: [confirmDeactivationIfNeeded],
				component: CastComponent,
			},
			{
				path: VotingRoutePath.Confirm,
				data: {
					navigateAwayAction:  NavigateAwayAction.ShowQuitProcessDialog,
				} as RouteData,
				canDeactivate: [confirmDeactivationIfNeeded],
				component: ConfirmComponent,
			},
		]
	},
	{
		path: VotingRoutePath.End,
		data: {
			headerOnly: true,
			reachablePaths: [VotingRoutePath.Init, VotingRoutePath.Confirm],
			navigateAwayAction:  NavigateAwayAction.GoToStartVotingPage,
		} as RouteData,
		canActivate: [areLegalTermsAccepted],
		canDeactivate: [confirmDeactivationIfNeeded],
		component: FinalizationPageComponent
	},
	{path: '**', redirectTo: ''},
];

@NgModule({
	imports: [
		RouterModule.forRoot(routes, {
			preloadingStrategy: PreloadAllModules,
			useHash: true,
			anchorScrolling: 'enabled',
			scrollPositionRestoration: 'top',
			onSameUrlNavigation: 'reload',
		}),
	],
	exports: [RouterModule],
})
export class AppRoutingModule {
}
