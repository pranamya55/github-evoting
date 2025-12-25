/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, inject, input } from '@angular/core';
import { ProcessCancellationService } from '@vp/voter-portal-ui-services';
import {CancelMode, FooterCancelMode} from "@vp/voter-portal-util-types";

@Component({
	selector: 'vp-footer',
	standalone: false,
	templateUrl: './footer.component.html',
})
export class FooterComponentComponent {
	cancelMode = input<FooterCancelMode>();
	private readonly cancellationProcessService = inject(ProcessCancellationService);

	confirmCancellation(): void {
		switch (this.cancelMode()) {
			case CancelMode.CancelVote:
				this.cancellationProcessService.cancelVote();
				break;
			case CancelMode.LeaveProcess:
				this.cancellationProcessService.leaveProcess();
				break;
		}
	}
}
