/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, DestroyRef, inject, OnInit} from '@angular/core';
import {Store} from "@ngrx/store";
import {getBackendError} from "@vp/voter-portal-ui-state";
import {BackendError, ErrorStatus, Nullable} from "@vp/voter-portal-util-types";
import {filter} from "rxjs/operators";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
	selector: 'vp-page-not-found',
	templateUrl: './page-not-found.component.html',
	standalone: false
})
export class PageNotFoundComponent implements OnInit {
	private readonly store = inject(Store);
	private readonly destroyRef: DestroyRef = inject(DestroyRef);
	error: Nullable<BackendError>;

	public ngOnInit(): void {
		this.store
			.select(getBackendError)
			.pipe(
				takeUntilDestroyed(this.destroyRef),
				filter((error) => {
					return (!error || error?.errorStatus === ErrorStatus.VotingClientTimeError)
				})
			)
			.subscribe((error) => {
				this.error = error ?? undefined;
			});
	}

}
