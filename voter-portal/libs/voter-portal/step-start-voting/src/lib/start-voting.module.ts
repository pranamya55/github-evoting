/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgbDatepickerModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';
import { UiComponentsModule } from '@vp/voter-portal-ui-components';
import { SharedStateModule } from '@vp/voter-portal-ui-state';
import { NgxMaskDirective, provideNgxMask } from 'ngx-mask';
import { ExtendedFactorComponent } from './extended-factor/extended-factor.component';
import { StartVotingComponent } from './start-voting/start-voting.component';
import { UiDirectivesModule } from '@vp/voter-portal-ui-directives';
import { RouterModule } from '@angular/router';
import {IconComponent} from "@vp/shared-ui-components";

@NgModule({
	imports: [
		// Warning, the order of the dependencies is important !
		// If the NgxMaskDirective is put 'at the wrong place', the input place older of textarea is not display when user input something.
		CommonModule,
		RouterModule,
		FormsModule,
		NgbDatepickerModule,
		NgxMaskDirective,
		IconComponent,
		ReactiveFormsModule,
		SharedStateModule,
		TranslateModule,
		UiComponentsModule,
		UiDirectivesModule
	],
	declarations: [StartVotingComponent, ExtendedFactorComponent],
	providers: [provideNgxMask({ validation: false })],
})
export class StartVotingModule {}
