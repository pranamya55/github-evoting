/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RouterLinkToDirective} from './router-link-to.directive';
import {RouterLink} from "@angular/router";
import {RoutingService} from "@sdm/shared-ui-services";
import {WorkflowStep} from "@sdm/shared-util-types";

describe('RouterLinkToDirective', () => {
  it('should create an instance', () => {
    const directive = new RouterLinkToDirective({} as RouterLink, {
      getLinkTo: (ws: WorkflowStep) => {
      }
    } as RoutingService);
    expect(directive).toBeTruthy();
  });
});
