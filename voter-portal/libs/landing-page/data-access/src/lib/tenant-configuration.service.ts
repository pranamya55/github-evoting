/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {ConfigurationService} from "@vp/landing-page-data-access";

@Injectable({
	providedIn: 'root',
})
export class TenantConfigurationService extends ConfigurationService {

	protected getConfigurationPath(): string {
		return `configuration/${this.getTenant()}`;
	}

	protected getCommonConfigurationPath(): string {
		return `configuration/`;
	}

	protected getTenant(): string {
		const hostname = window.location.hostname;
		let tenant = '';
		if (hostname && hostname !== 'localhost') {
			tenant = hostname.split('.')[0];
		}
		return tenant;
	}
}
