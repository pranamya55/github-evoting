/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { UiComponentsModule } from '@vp/voter-portal-ui-components';
import { ConfirmComponent } from './confirm/confirm.component';
import { FormsModule } from '@angular/forms';
import {IconComponent} from "@vp/shared-ui-components";

@NgModule({
	imports: [CommonModule, TranslateModule, UiComponentsModule, FormsModule, IconComponent],
	declarations: [ConfirmComponent],
})
export class ConfirmModule {}
