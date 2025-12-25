/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {LocationStrategy} from '@angular/common';
import {AfterViewInit, Component, HostBinding, HostListener, inject, OnInit, ViewChild} from '@angular/core';
import {Store} from '@ngrx/store';
import {TranslateService} from '@ngx-translate/core';
import {ConfigurationService, ProcessCancellationService} from '@vp/voter-portal-ui-services';
import {getIsAuthenticated, LanguageSelectorActions, SharedActions} from '@vp/voter-portal-ui-state';
import {Observable, partition} from 'rxjs';
import {filter, map, take} from 'rxjs/operators';
import {ActivatedRoute, ActivatedRouteSnapshot, Router, RouterOutlet} from "@angular/router";
import {environment} from "@vp/voter-portal-data-access";
import {VotingRoutePath} from "@vp/voter-portal-util-types";

declare global {
	interface Window {
		Cypress?: object;
		store?: Store;
	}
}

@Component({
	selector: 'vp-root',
	templateUrl: './app.component.html',
	standalone: false
})
export class AppComponent implements OnInit, AfterViewInit {
	readonly configuration = inject(ConfigurationService);
	private readonly route = inject(ActivatedRoute);
	private readonly store = inject(Store);
	private readonly translate = inject(TranslateService);
	private readonly cancellationService = inject(ProcessCancellationService);
	private readonly location = inject(LocationStrategy);
	private readonly router = inject(Router);

	@ViewChild(RouterOutlet) routerOutlet!: RouterOutlet;
	childRouteSnapshot!: ActivatedRouteSnapshot;
	showHeaderOnly = false;
	noHeader = false;

	get isCurrentlyAuthenticated$(): Observable<boolean> {
		return this.store.select(getIsAuthenticated).pipe(take(1));
	}

	ngOnInit() {
		if (window.Cypress) {
			window.store = this.store;
		}

		this.location.onPopState(() => {
			this.cancellationService.backButtonPressed = true;
			return false;
		});

		this.store.dispatch(SharedActions.serverErrorCleared());

		this.isCurrentlyAuthenticated$.pipe(filter(Boolean)).subscribe(() => {
			this.store.dispatch(SharedActions.loggedOut());
		});

		this.route.queryParamMap
			.pipe(
				map(params => params.get('lang')?.toUpperCase()),
				filter((lang): lang is string => !!lang),
				map((lang) => {
					if (environment.availableLanguages.map(av => av.id).includes(lang)) {
						return lang;
					}
					return environment.defaultLang;
				}),
			)
			.subscribe(lang => {
				this.store.dispatch(LanguageSelectorActions.languageSelected({lang}));
			});
	}

	ngAfterViewInit() {
		this.routerOutlet.activateEvents.subscribe(() => {
			this.childRouteSnapshot = this.routerOutlet.activatedRoute.snapshot;
			this.showHeaderOnly = !!this.childRouteSnapshot.data['headerOnly'];
			this.noHeader = !!this.childRouteSnapshot.data['noHeader'];
		})
	}

	@HostListener('window:beforeunload', ['$event']) beforeUnload(
		$event: BeforeUnloadEvent
	) {
		if (this.router.url === '/' + VotingRoutePath.End) return;

		const [isAuthenticated$, isNotAuthenticated$] = partition(
			this.isCurrentlyAuthenticated$,
			Boolean
		);

		isAuthenticated$.subscribe(() => {
			$event.returnValue = this.translate.instant(
				'common.questionnavigateaway'
			);
		});

		isNotAuthenticated$.subscribe(() => {
			$event.preventDefault();
		});
	}
}
