/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import { Component, Input, OnDestroy, OnInit, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { ConfigurationService } from '@vp/voter-portal-ui-services';
import { getBackendError, getLoading } from '@vp/voter-portal-ui-state';
import { BackendError, ErrorStatus, Nullable } from '@vp/voter-portal-util-types';
import { Observable, Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';

function matches(
	error: BackendError,
	errorMessages: ErrorStatus | ErrorStatus[],
): boolean {
	return typeof errorMessages === 'string'
		? error.errorStatus === errorMessages
		: errorMessages.includes(error.errorStatus as ErrorStatus);
}

@Component({
	selector: 'vp-backenderror',
	templateUrl: './backend-error.component.html',
	standalone: false,
})
export class BackendErrorComponent implements OnInit, OnDestroy {
	readonly translate = inject(TranslateService);
	private readonly configuration = inject(ConfigurationService);
	private readonly store = inject(Store);

	@Input() exclude: ErrorStatus | ErrorStatus[] = [];
	@Input() includeOnly: ErrorStatus | ErrorStatus[] =
		Object.values(ErrorStatus);
	@Input() alertId: string | undefined;
	loading$: Observable<boolean> = this.store.select(getLoading);
	error: Nullable<BackendError>;
	private subscription$!: Subscription;

	public ngOnInit(): void {
		this.subscription$ = this.store
			.select(getBackendError)
			.pipe(
				filter((error) => {
					return (
						!error ||
						(matches(error, this.includeOnly) && !matches(error, this.exclude))
					);
				}),
			)
			.subscribe((error) => {
				this.error = error ?? undefined;
			});
	}

	public ngOnDestroy(): void {
		this.subscription$.unsubscribe();
	}

	getErrorMessageKey({ errorStatus }: BackendError) {
		switch (errorStatus) {
			case ErrorStatus.StartVotingKeyInvalid:
			case ErrorStatus.BallotBoxEnded:
			case ErrorStatus.BallotBoxNotStarted:
			case ErrorStatus.AuthenticationAttemptsExceeded:
			case ErrorStatus.VotingCardBlocked:
			case ErrorStatus.ConnectionError:
			case ErrorStatus.ConfirmationKeyIncorrect:
			case ErrorStatus.ConfirmationKeyInvalid:
			case ErrorStatus.ConfirmationAttemptsExceeded:
			case ErrorStatus.VoteInvalid:
			case ErrorStatus.TimestampMisaligned:
				return `backenderror.${errorStatus}`;
			case ErrorStatus.ExtendedFactorInvalid:
				return `backenderror.${errorStatus}.${this.configuration.identification}`;
			default:
				return `backenderror.${ErrorStatus.Default}`;
		}
	}
}
