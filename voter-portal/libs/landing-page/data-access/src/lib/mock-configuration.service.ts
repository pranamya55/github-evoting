/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Injectable} from '@angular/core';
import {ConfigurationService} from "./configuration.service";

@Injectable({
	providedIn: 'root',
})
export class MockConfigurationService extends ConfigurationService {

	protected getConfigurationPath(): string {
		return `mock-configuration/${this.getTenant()}`;
	}

	protected getCommonConfigurationPath(): string {
		return `mock-configuration/common/`;
	}

	protected getTenant(): string {
		const params = new URLSearchParams(window.location.search);
		return params.get('tenant') || 'pit';
	}
}

export {MockConfigurationService as TenantConfigurationService}
