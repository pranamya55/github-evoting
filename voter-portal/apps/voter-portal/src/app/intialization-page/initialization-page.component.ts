/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, inject, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {InitializationActions} from "@vp/voter-portal-ui-state";
import {Store} from "@ngrx/store";

@Component({
	selector: 'vp-initialization-page',
	templateUrl: './initialization-page.component.html',
	standalone: false
})
export class InitializationPageComponent implements OnInit {
	private readonly route = inject(ActivatedRoute);
	private readonly store = inject(Store);

	ngOnInit(): void {
		const electionEventId = this.route.snapshot.paramMap.get(
			'electionEventId'
		) as string;

		this.store.dispatch(InitializationActions.initializationLoading({electionEventId}));
	}
}
