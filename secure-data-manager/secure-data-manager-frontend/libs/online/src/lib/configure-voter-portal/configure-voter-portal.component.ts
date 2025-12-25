/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component, inject} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {WorkflowStateService} from '@sdm/shared-ui-services';
import {VoterPortalConfigStatus, VoterPortalSdmConfig, WorkflowStatus, WorkflowStep} from '@sdm/shared-util-types';
import {map, switchMap} from 'rxjs';
import {NextButtonComponent, PageActionsComponent, PageTitleComponent} from '@sdm/shared-ui-components';
import {ProgressComponent} from "@sdm/shared-feature-progress";
import {ConfigureVoterPortalService} from "./configure-voter-portal.service";
import {ClipboardModule} from "ngx-clipboard";
import {takeUntilDestroyed} from "@angular/core/rxjs-interop";

@Component({
	selector: 'sdm-configure-voter-portal',
	standalone: true,
	imports: [
		CommonModule,
		FormsModule,
		TranslateModule,
		PageActionsComponent,
		NextButtonComponent,
		ProgressComponent,
		ClipboardModule,
		PageTitleComponent,
	],
	templateUrl: './configure-voter-portal.component.html',
})
export class ConfigureVoterPortalComponent {
	protected readonly WorkflowStatus = WorkflowStatus;
	voterPortalConfig!: VoterPortalSdmConfig | undefined;
	workflowStepStatus!: WorkflowStatus;
	copied = false;
	voterPortalConfigStatus = VoterPortalConfigStatus;

	forceInProgress = true;
	forceError = false;
	forceWarning = false;

	faviconNotFound = false;
	logoNotFound = false;
	configNotFound = false;

	isUploadEnabled = false;

	private readonly workflowStates = inject(WorkflowStateService);
	private readonly configureVoterPortalService = inject(ConfigureVoterPortalService);
	private readonly translate = inject(TranslateService);

	constructor() {
		this.cleanInProgress();
		this.workflowStates
			.get(WorkflowStep.ConfigureVoterPortal)
			.pipe(
				switchMap((state) =>
					this.configureVoterPortalService
						.getConfiguration()
						.pipe(
							map((config) => ({state, config}))
						)
				),
				takeUntilDestroyed()
			)
			.subscribe(({state, config}) => {
				this.voterPortalConfig = config;
				this.workflowStepStatus = state.status;
				this.updateIsUploadEnabled();
			});
	}

	fetchConfiguration(): void {
		this.cleanInProgress();
		this.configureVoterPortalService.getConfiguration().subscribe((value) => {
			this.voterPortalConfig = value;
			this.updateIsUploadEnabled();
		});
	}

	get voterPortalConfigJson(): string | undefined {
		return this.voterPortalConfig?.payload?.config
			? atob(this.voterPortalConfig.payload.config)
			: undefined;
	}

	updateIsUploadEnabled(): void {
		if (!this.voterPortalConfigJson && this.workflowStepStatus !== WorkflowStatus.InProgress) {
			this.setStatus(false, true, false, true, false);
			return;
		}

		if (this.extendedAuthenticationFactor === this.translate.instant('configureVoterPortal.extendedAuthenticationFactor.unknown') || this.displayWriteInFAQ === this.translate.instant('configureVoterPortal.extendedAuthenticationFactor.unknown')) {
			this.setStatus(false, true, false, true, false);
			return;
		}

		const {config, favicon, logo} = this.voterPortalConfig?.state || {};
		if ([config, favicon, logo].includes(VoterPortalConfigStatus.NotFound)) {
			this.setStatus(false, true, false, true, false);
			return;
		}

		if ([config, favicon, logo].includes(VoterPortalConfigStatus.NotSynchronized)) {
			this.setStatus(false, false, true, false, true);
			return;
		}

		this.setStatus(false, false, false, false, false);
	}

	get isAllSynchronized(): boolean {
		const {config, favicon, logo} = this.voterPortalConfig?.state || {};
		return [config, favicon, logo].every((status) => status === VoterPortalConfigStatus.Synchronized);
	}

	get isAllFounded(): boolean {
		const {config, favicon, logo} = this.voterPortalConfig?.state || {};
		return [config, favicon, logo].every((status) => status !== VoterPortalConfigStatus.NotFound);
	}

	get extendedAuthenticationFactor(): string {
		try {
			const configJson = this.voterPortalConfigJson ? JSON.parse(this.voterPortalConfigJson) : null;
			const identification = configJson?.identification;

			if (identification === 'yob') {
				return this.translate.instant('configureVoterPortal.extendedAuthenticationFactor.yob');
			}
			if (identification === 'dob') {
				return this.translate.instant('configureVoterPortal.extendedAuthenticationFactor.dob');
			}
		} catch (error) {
			console.error('Error parsing the configuration JSON file for extended authentication factor.');
		}
		return this.translate.instant('configureVoterPortal.extendedAuthenticationFactor.unknown');
	}

	get displayWriteInFAQ(): string {
		try {
			const configJson = this.voterPortalConfigJson ? JSON.parse(this.voterPortalConfigJson) : null;
			const writeIns = configJson?.contestsCapabilities?.writeIns;

			if (writeIns === true) {
				return this.translate.instant('configureVoterPortal.displayWriteInFAQ.show');
			} else if (writeIns === false) {
				return this.translate.instant('configureVoterPortal.displayWriteInFAQ.showNot');
			}
		} catch (error) {
			console.error('Error parsing the configuration JSON file for display writeIn FAQ.');
		}
		return this.translate.instant('configureVoterPortal.displayWriteInFAQ.unknown');
	}


	uploadVoterPortalConfiguration(): void {
		if (this.isUploadEnabled) {
			this.configureVoterPortalService.configureVoterPortal();
		}
	}

	setCopied(): void {
		this.copied = true;
		setTimeout(() => (this.copied = false), 10000);
	}

	private setStatus(
		forceInProgress: boolean,
		forceError: boolean,
		forceWarning: boolean,
		configNotFound: boolean,
		isUploadEnabled: boolean
	): void {
		this.forceInProgress = forceInProgress;
		this.forceError = forceError;
		this.forceWarning = forceWarning;
		this.configNotFound = configNotFound;
		this.isUploadEnabled = isUploadEnabled;
	}

	cleanInProgress(): void {
		this.forceInProgress = true;
		this.voterPortalConfig = undefined;
		this.setStatus(true, false, false, false, false);
	}
}
