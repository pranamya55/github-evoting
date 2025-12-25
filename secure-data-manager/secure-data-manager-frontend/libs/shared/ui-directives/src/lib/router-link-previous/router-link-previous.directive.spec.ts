/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {RouterLinkPreviousDirective} from './router-link-previous.directive';
import {RouterLink} from "@angular/router";
import {RoutingService} from "@sdm/shared-ui-services";

describe('RouterLinkPreviousDirective', () => {
  it('should create an instance', () => {
    const directive = new RouterLinkPreviousDirective({} as RouterLink, {
      getLinkToPreviousStep: () => {
      }
    } as RoutingService);
    expect(directive).toBeTruthy();
  });
});
