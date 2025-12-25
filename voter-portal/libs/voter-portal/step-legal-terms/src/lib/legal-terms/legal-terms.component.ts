/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, DestroyRef, inject, OnInit, ViewChild,} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Store} from '@ngrx/store';
import {TranslateService} from '@ngx-translate/core';
import {ConfigurationService, ProcessCancellationService} from '@vp/voter-portal-ui-services';
import {getHasAcceptedLegalTerms, LegalTermsActions} from '@vp/voter-portal-ui-state';
import {NgForm} from '@angular/forms';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {focusFirstInvalidControl} from "@vp/voter-portal-util-helpers";

@Component({
	selector: 'vp-legal-terms',
	templateUrl: './legal-terms.component.html',
	standalone: false,
})
export class LegalTermsComponent implements OnInit {
	@ViewChild('agreementForm') agreementForm!: NgForm;
	private readonly route = inject(ActivatedRoute);
	private readonly store = inject(Store);
	private readonly translate = inject(TranslateService);
	private readonly destroyRef = inject(DestroyRef);
	private readonly cancelProcessService = inject(ProcessCancellationService);
	protected readonly configuration = inject(ConfigurationService);

	electionEventId!: string;

	ngOnInit(): void {
		this.electionEventId = this.route.snapshot.paramMap.get(
			'electionEventId',
		) as string;

		this.translate.onLangChange
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe(() => {
				// reset all checkboxes to be unchecked
				this.agreementForm.reset();
			});
	}

	get agreed(): boolean {
		return !!this.store.selectSignal(getHasAcceptedLegalTerms)();
	}

	agree(): void {
		if (this.agreementForm.invalid) {
			focusFirstInvalidControl();
			return;
		}

		this.cancelProcessService.reset();
		this.store.dispatch(LegalTermsActions.agreeClicked());
	}
}
