/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { ConfirmationModalComponent } from './confirmation-modal/confirmation-modal.component';
import { UiDirectivesModule } from "@vp/voter-portal-ui-directives";
import { MarkdownPipe} from "e-voting-libraries-ui-kit";
import { IconComponent  } from "@vp/shared-ui-components";

@NgModule({
	imports: [CommonModule, TranslateModule, UiDirectivesModule, MarkdownPipe, IconComponent],
	declarations: [ConfirmationModalComponent],
	exports: [ConfirmationModalComponent],
})
export class UiConfirmationModule {}
