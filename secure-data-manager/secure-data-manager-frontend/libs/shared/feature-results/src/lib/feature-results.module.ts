/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {ReactiveFormsModule} from '@angular/forms';
import {TranslateModule} from '@ngx-translate/core';
import {NgbAccordionModule, NgbCollapseModule} from '@ng-bootstrap/ng-bootstrap';
import {ResultsModalComponent} from "@sdm/shared-feature-results";
import {ResultsComponent} from "e-voting-libraries-ui-kit";

@NgModule({
	imports: [
		CommonModule,
		ReactiveFormsModule,
		TranslateModule,
		NgbAccordionModule,
		TranslateModule,
		NgbCollapseModule,
		ResultsComponent
	],
	declarations: [
		ResultsModalComponent
	],
	exports: [ResultsModalComponent]
})
export class FeatureResultsModule {
}
