/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {inject} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {ImageService} from "./image.service";
import {ExternalHeaderConfiguration} from "e-voting-libraries-ui-kit";
import {BehaviorSubject, combineLatest, map, Observable, ReplaySubject} from "rxjs";
import {
	ContentElement,
	EventElement,
	FooterElement,
	LanguageConfiguration, ModeElement,
	PageConfiguration,
	SharedContentElement, TenantState, TenantStateEvent
} from "@vp/landing-page-utils-types";

export abstract class ConfigurationService {
	private readonly http = inject(HttpClient);
	private readonly imageService: ImageService = inject(ImageService);
	private readonly faviconSubject = new BehaviorSubject<string>("");
	private readonly pageConfigurationSubject = new ReplaySubject<PageConfiguration>(1);
	private readonly eventsSubject = new ReplaySubject<EventElement[]>(1);
	private readonly statesSubject = new ReplaySubject<TenantState[]>(1);
	private readonly sharedSubject = new ReplaySubject<SharedContentElement[]>(1);
	private readonly modeSubject = new ReplaySubject<ModeElement[]>(1);

	protected abstract getConfigurationPath(): string;
	protected abstract getCommonConfigurationPath(): string;
	protected abstract getTenant(): string;

	protected readonly faviconFilename: string = 'favicon.ico';
	protected readonly logoFilename: string = 'logo.png';

	protected readonly pageConfigurationFilename: string = 'landing-page-configuration.json';
	protected readonly eventConfigurationFilename: string = 'landing-page-event-configuration.json';

	protected readonly stateConfigurationFilename: string = 'landing-page-state-configuration.json';
	protected readonly sharedConfigurationFilename: string = 'landing-page-shared-configuration.json';
	protected readonly modeConfigurationFilename: string = 'landing-page-mode-configuration.json';

	constructor() {
		const configurationPath = this.getConfigurationPath();
		const commonConfigurationPath = this.getCommonConfigurationPath();

		// Tenant specific configuration
		const configuration$ = this.http.get<PageConfiguration>(`${configurationPath}/${this.pageConfigurationFilename}`);
		const events$ = this.http.get<EventElement[]>(`${configurationPath}/${this.eventConfigurationFilename}`);
		const logoBase64$ = this.imageService.loadBase64(`${configurationPath}/${this.logoFilename}`);
		const faviconBase64$ = this.imageService.loadBase64(`${configurationPath}/${this.faviconFilename}`);
		// Common configuration
		const states$ = this.http.get<TenantState[]>(`${commonConfigurationPath}/${this.stateConfigurationFilename}`);
		const shared$ = this.http.get<SharedContentElement[]>(`${commonConfigurationPath}/${this.sharedConfigurationFilename}`);
		const mode$ = this.http.get<ModeElement[]>(`${commonConfigurationPath}/${this.modeConfigurationFilename}`);

		combineLatest([configuration$, events$, logoBase64$, faviconBase64$, states$, shared$, mode$]).subscribe(
			([configuration, events, logoBase64, faviconBase64, states, shared, mode]) => {
				const headerConfig = configuration.headerConfiguration as ExternalHeaderConfiguration;
				headerConfig.logo.src = logoBase64;
				this.pageConfigurationSubject.next(configuration);
				this.eventsSubject.next(events);
				this.faviconSubject.next(faviconBase64);
				this.statesSubject.next(states);
				this.sharedSubject.next(shared);
				this.modeSubject.next(mode);
			}
		);
	}

	getContentConfiguration(): Observable<ContentElement[]> {
		return this.pageConfigurationSubject.asObservable().pipe(
			map(pageConfiguration => pageConfiguration.contentConfiguration)
		);
	}

	getHeaderConfiguration(): Observable<ExternalHeaderConfiguration> {
		return this.pageConfigurationSubject.asObservable().pipe(
			map(pageConfiguration => pageConfiguration.headerConfiguration as ExternalHeaderConfiguration)
		);
	}

	getFooterConfiguration(): Observable<FooterElement> {
		return this.pageConfigurationSubject.asObservable().pipe(
			map(pageConfiguration => pageConfiguration.footerConfiguration as FooterElement)
		);
	}

	getLanguageConfiguration(): Observable<LanguageConfiguration> {
		return this.pageConfigurationSubject.asObservable().pipe(
			map(pageConfiguration => pageConfiguration.languageConfiguration as LanguageConfiguration)
		);
	}

	getFavicon(): Observable<string> {
		return this.faviconSubject.asObservable();
	}

	getEvents(): Observable<EventElement[]> {
		return this.eventsSubject.asObservable();
	}

	getStates(): Observable<TenantStateEvent[]> {
		return this.statesSubject.asObservable().pipe(
			map(states => states.filter(state => state.tenant === this.getTenant())),
			map(states => states.length > 0 ? states[0].events : [])
		);
	}

	getShared(): Observable<SharedContentElement[]> {
		return this.sharedSubject.asObservable();
	}

	getMode(): Observable<ModeElement> {
		const tenantState$ = this.statesSubject.asObservable().pipe(
			map(states => states.find(state => state.tenant === this.getTenant()))
		);
		return combineLatest([this.modeSubject.asObservable(), tenantState$]).pipe(
			map(([modes, state]) => modes.find(mode => mode.id === state?.activeMode) as ModeElement)
		);
	}

}
