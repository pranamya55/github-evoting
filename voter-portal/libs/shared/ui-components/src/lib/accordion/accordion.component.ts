/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input} from '@angular/core';
import {NgbAccordionModule} from "@ng-bootstrap/ng-bootstrap";
import {CommonModule} from "@angular/common";
import {TranslatePipe} from "@ngx-translate/core";

type NgClass = string | string[] | Set<string> | { [className: string]: any };

@Component({
	selector: 'vp-accordion',
	standalone: true,
	imports: [
		CommonModule,
		NgbAccordionModule,
		TranslatePipe
	],
	templateUrl: './accordion.component.html'
})
export class AccordionComponent {
	@Input({ required: true }) headingLevel!: number;
	@Input() collapsed: boolean = false;
	@Input() containerClass?: NgClass;
	@Input() bodyClass?: NgClass;
}
