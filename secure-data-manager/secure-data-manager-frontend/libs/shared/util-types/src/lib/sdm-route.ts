/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Data, Route} from '@angular/router';
import {WorkflowStep} from './workflow-step';

export interface SdmRoute extends Route {
  data?: SdmRouteData;
  children?: SdmRoute[];
}

interface SdmRouteData extends Data {
  workflowStep: WorkflowStep;
  nextStep?: WorkflowStep;
  exchangeIndex?: string;
}
