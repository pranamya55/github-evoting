/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {inject} from '@angular/core';
import {CanActivateChildFn, Router} from '@angular/router';
import {WorkflowStatus} from '@sdm/shared-util-types';
import {map} from 'rxjs';
import {WorkflowStateService} from './workflow-state.service';
import {RoutingService} from "../routing/routing.service";

export const workflowGuard: CanActivateChildFn = (childRoute, routerState) => {
	const url = childRoute.url.map(urlSegment => `/${urlSegment.path}`).join();
	const isIntermediary = !new RegExp(`${url}$`).test(routerState.url);
	if (isIntermediary) return true;

	const workflowStates = inject(WorkflowStateService);
	const routingService = inject(RoutingService);
	const router = inject(Router);

	return workflowStates.getSnapshot().pipe(
		map(stateList => {
			// allow navigation if the requested step is not idle (i.e. step is ready, in progress, complete or in error)
			const childStep = childRoute.data?.['workflowStep'];
			const childStepState = childStep && stateList.find(({step}) => step === childStep);
			if (childStepState && childStepState.status !== WorkflowStatus.Idle) return true;

			// allow navigation to an empty URL (i.e. goodbye page) if all steps are completed
			const isUrlEmpty = !childRoute.url.length;
			const allStepsCompleted = isUrlEmpty && stateList
				.filter(({optional}) => !optional)
				.every(({status}) => status === WorkflowStatus.Complete);
			if (allStepsCompleted) return true;

			// redirect to the current step (i.e. ready, in progress or in error) otherwise
			const currentStepState = stateList
				.find(({optional, status}) => !optional && status !== WorkflowStatus.Idle && status !== WorkflowStatus.Complete);
			const linkToIdleStep = currentStepState && routingService.getLinkTo(currentStepState.step);
			if (linkToIdleStep) return router.createUrlTree(linkToIdleStep);

			return false;
		})
	);
};
