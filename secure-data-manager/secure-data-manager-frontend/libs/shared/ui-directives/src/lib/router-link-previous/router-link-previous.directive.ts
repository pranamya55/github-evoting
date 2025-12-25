/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Directive} from '@angular/core';
import {RouterLink} from '@angular/router';
import {RoutingService} from 'libs/shared/ui-services/src/lib/routing/routing.service';

@Directive({
  selector: '[sdmRouterLinkPrevious]',
  standalone: true,
  hostDirectives: [RouterLink]
})
export class RouterLinkPreviousDirective {
  constructor(
    readonly routerLink: RouterLink,
    readonly routingService: RoutingService,
  ) {
    this.routerLink.routerLink = this.routingService.getLinkToPreviousStep();
  }
}
