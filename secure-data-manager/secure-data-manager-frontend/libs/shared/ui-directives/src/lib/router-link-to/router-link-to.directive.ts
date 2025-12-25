/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Directive, Input} from '@angular/core';
import {RouterLink} from '@angular/router';
import {WorkflowStep} from '@sdm/shared-util-types';
import {RoutingService} from 'libs/shared/ui-services/src/lib/routing/routing.service';

@Directive({
  selector: '[sdmRouterLinkTo]',
  standalone: true,
  hostDirectives: [RouterLink]
})
export class RouterLinkToDirective {
  @Input({required: true}) sdmRouterLinkTo?: WorkflowStep;

  constructor(
    readonly routerLink: RouterLink,
    readonly routingService: RoutingService
  ) {
    if (this.sdmRouterLinkTo) this.routerLink.routerLink = this.routingService.getLinkTo(this.sdmRouterLinkTo);
  }
}
