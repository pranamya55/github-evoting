/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CastComponent } from './cast/cast.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NgxMaskDirective, provideNgxMask } from 'ngx-mask';
import { TranslateModule } from '@ngx-translate/core';
import { UiComponentsModule } from '@vp/voter-portal-ui-components';
import { UiDirectivesModule } from '@vp/voter-portal-ui-directives';
import { RouterLink } from '@angular/router';
import {IconComponent} from "@vp/shared-ui-components";
import {MarkdownPipe} from "e-voting-libraries-ui-kit";

@NgModule({
	imports: [
		// Warning, the order of the dependencies is important !
		// If the NgxMaskDirective is put 'at the wrong place', the input place older of textarea is not displayed when the user inputs something.
		CommonModule,
		NgxMaskDirective,
		FormsModule,
		ReactiveFormsModule,
		TranslateModule,
		UiComponentsModule,
		UiDirectivesModule,
		RouterLink,
		IconComponent,
		MarkdownPipe,
	],
	declarations: [CastComponent],
	providers: [provideNgxMask({ validation: false })],
})
export class CastModule {}
