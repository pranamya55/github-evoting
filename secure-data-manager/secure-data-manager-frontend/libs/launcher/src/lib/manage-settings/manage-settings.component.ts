/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {Settings} from '@sdm/shared-util-types';
import {SeedValidator} from './seed.validator';
import {PasswordValidators} from '@sdm/shared-feature-passwords';
import {PageTitleComponent, PolicyComponent} from '@sdm/shared-ui-components';
import {NgxMaskDirective} from 'ngx-mask';

@Component({
	selector: 'sdm-select-settings',
	standalone: true,
	imports: [
		CommonModule,
		TranslateModule,
		ReactiveFormsModule,
		PolicyComponent,
		NgxMaskDirective,
		PageTitleComponent,
	],
	templateUrl: './manage-settings.component.html',
})
export class ManageSettingsComponent implements OnInit {
	settings: Settings | null = null;
	startingSDM = false;
	workspaceValid = true;
	hidePassword = true;
	useProxy = false;

	chunkSizes = [
		{value: '100', label: 'normalElectionEvent'},
		{value: '20', label: 'bigElectionEvent'},
	];
	passwordPolicies = [
		'length',
		'digit',
		'specialChar',
		'lowerCaseChar',
		'uppercaseChar',
	];
	electionEventSeedPolicies = ['prefix', 'date', 'suffix', 'delimiter'];
	sdmModes: string[] = ['SETUP', 'ONLINE', 'TALLY'];

	settingsForm: FormGroup<{
		electionEventSeed: FormControl<string>;
		mode: FormControl<string>;
		voterPortalHost: FormControl<string>;
		votingServerHost: FormControl<string>;
		twoWaySSLLocation: FormControl<string>;
		twoWaySSLPwdLocation: FormControl<string>;
		outputFolder: FormControl<string>;
		externalConfigurationFolder: FormControl<string>;
		printFolder: FormControl<string>;
		verifierFolder: FormControl<string>;
		tallyFolder: FormControl<string>;
		workspaceFolder: FormControl<string>;
		voterPortalConfigurationFolder: FormControl<string>;
		password: FormControl<string>;
		choiceCodeGenerationChunkSize: FormControl<number>;
		directTrustLocation: FormControl<string>;
		directTrustPwdLocation: FormControl<string>;
		proxyConfig: FormControl<string>;
	}>;

	constructor(
		private readonly router: Router,
		private readonly fb: FormBuilder,
	) {
		this.settingsForm = this.fb.group(
			{
				electionEventSeed: [
					'',
					{validators: [Validators.required, SeedValidator.validate()]},
				],
				mode: ['', {validators: [Validators.required]}],
				voterPortalHost: [''],
				votingServerHost: [''],
				twoWaySSLLocation: [''],
				twoWaySSLPwdLocation: [''],
				workspaceFolder: ['', {validators: [Validators.required]}],
				outputFolder: [''],
				externalConfigurationFolder: [''],
				printFolder: [''],
				verifierFolder: [''],
				tallyFolder: [''],
				voterPortalConfigurationFolder: [''],
				password: [
					'',
					{validators: [Validators.required, PasswordValidators.validate]},
				],
				choiceCodeGenerationChunkSize: [100],
				directTrustLocation: [''],
				directTrustPwdLocation: [''],
				proxyConfig: [''],
			},
			{
				validators: this.formControlValidator.bind(this)
			}
		);
	}

	ngOnInit(): void {
		this.settings = history.state['settings'];
		this.settingsForm.patchValue({
			electionEventSeed: this.settings?.electionEventSeed,
			mode: this.settings?.mode,
			voterPortalHost: this.settings?.voterPortalHost,
			votingServerHost: this.settings?.votingServerHost,
			workspaceFolder: this.settings?.workspaceFolder,
			externalConfigurationFolder: this.settings?.externalConfigurationFolder,
			outputFolder: this.settings?.outputFolder,
			printFolder: this.settings?.printFolder,
			verifierFolder: this.settings?.verifierFolder,
			tallyFolder: this.settings?.tallyFolder,
			voterPortalConfigurationFolder: this.settings?.voterPortalConfigurationFolder,
			password: this.settings?.exportPwd,
			choiceCodeGenerationChunkSize:
			this.settings?.choiceCodeGenerationChunkSize,
			directTrustLocation: this.settings?.directTrustLocation,
			directTrustPwdLocation: this.settings?.directTrustPwdLocation,
			twoWaySSLLocation: this.settings?.twoWaySSLLocation,
			twoWaySSLPwdLocation: this.settings?.twoWaySSLPwdLocation,
			proxyConfig: this.settings?.proxyConfig,
		});
		this.useProxy = !!this.settings?.proxyConfig;
	}

	get mode(): FormControl<string> {
		return this.settingsForm.controls.mode;
	}

	get password(): FormControl<string> {
		return this.settingsForm.controls.password;
	}

	passwordHasError(policy: string): boolean {
		return (
			!!this.settingsForm.controls.password.errors?.[policy] ||
			this.settingsForm.controls.password.value === ''
		);
	}

	electionEventSeedHasError(policy: string): boolean {
		if (
			!this.settingsForm.controls.electionEventSeed.value ||
			this.settingsForm.controls.electionEventSeed.value === ''
		)
			return true;

		const errors = this.settingsForm.controls.electionEventSeed.errors;
		if (!errors?.['seed']) return false;

		return errors['seed'][policy];
	}

	previous() {
		this.router.navigate(['select-settings']);
	}

	startSDM() {
		this.saveSettings(true);
	}

	save() {
		this.saveSettings(false);
		this.delay(100).then(() => this.router.navigate(['select-settings']));
	}

	delete() {
		window.SettingsApi?.deleteSettings(this.settings?.id ?? -1);
		this.delay(100).then(() => this.router.navigate(['select-settings']));
	}

	inputTransformFn = (value: unknown): string => {
		return typeof value === 'string' ? value.toUpperCase() : String(value);
	};

	outputTransformFn = (value: string | number | null | undefined): string => {
		return value ? String(value).toUpperCase() : '';
	};

	private formControlValidator(form: FormGroup) {
		const mode = form.controls['mode'].value;

		if (mode === 'SETUP') {
			return this.setupFormControlValidator(form);
		}

		if (mode === 'ONLINE') {
			return this.onlineFormControlValidator(form);
		}

		if (mode === 'TALLY') {
			return this.tallyFormControlValidator(form);
		}

		return null;
	}

	setupFormControlValidator(form: FormGroup) {
		const directTrustLocation = form.controls['directTrustLocation'].value;
		if (!directTrustLocation || directTrustLocation.trim() === '') {
			return {directTrustLocationRequired: true};
		}

		const directTrustPwdLocation = form.controls['directTrustPwdLocation'].value;
		if (!directTrustPwdLocation || directTrustPwdLocation.trim() === '') {
			return {directTrustPwdLocationRequired: true};
		}

		const outputFolder = form.controls['outputFolder'].value;
		if (!outputFolder || outputFolder.trim() === '') {
			return {outputFolderRequired: true};
		}

		const externalConfigurationFolder = form.controls['externalConfigurationFolder'].value;
		if (!externalConfigurationFolder || externalConfigurationFolder.trim() === '') {
			return {externalConfigurationFolderRequired: true};
		}

		const printFolder = form.controls['printFolder'].value;
		if (!printFolder || printFolder.trim() === '') {
			return {printFolderRequired: true};
		}

		const verifierFolder = form.controls['verifierFolder'].value;
		if (!verifierFolder || verifierFolder.trim() === '') {
			return {verifierFolderRequired: true};
		}

		const choiceCodeGenerationChunkSize = form.controls['choiceCodeGenerationChunkSize'].value;
		if (!choiceCodeGenerationChunkSize || choiceCodeGenerationChunkSize.trim() === '') {
			return {choiceCodeGenerationChunkSizeRequired: true};
		}

		return null;
	}

	private onlineFormControlValidator(form: FormGroup) {
		const voterPortalHost = form.controls['voterPortalHost'].value;
		if (!voterPortalHost || voterPortalHost.trim() === '') {
			return {voterPortalHost: true};
		}

		const votingServerHost = form.controls['votingServerHost'].value;
		if (!votingServerHost || votingServerHost.trim() === '') {
			return {votingServerHost: true};
		}

		const outputFolder = form.controls['outputFolder'].value;
		if (!outputFolder || outputFolder.trim() === '') {
			return {outputFolderRequired: true};
		}

		const voterPortalConfigurationFolder = form.controls['voterPortalConfigurationFolder'].value;
		if (!voterPortalConfigurationFolder || voterPortalConfigurationFolder.trim() === '') {
			return {voterPortalConfigurationFolder: true};
		}

		return null;
	}

	private tallyFormControlValidator(form: FormGroup) {
		const directTrustLocation = form.controls['directTrustLocation'].value;
		if (!directTrustLocation || directTrustLocation.trim() === '') {
			return {directTrustLocationRequired: true};
		}

		const directTrustPwdLocation = form.controls['directTrustPwdLocation'].value;
		if (!directTrustPwdLocation || directTrustPwdLocation.trim() === '') {
			return {directTrustPwdLocationRequired: true};
		}

		const verifierFolder = form.controls['verifierFolder'].value;
		if (!verifierFolder || verifierFolder.trim() === '') {
			return {verifierFolderRequired: true};
		}

		const tallyFolder = form.controls['tallyFolder'].value;
		if (!tallyFolder || tallyFolder.trim() === '') {
			return {tallyFolderRequired: true};
		}

		return null;
	}

	private delay(ms: number) {
		return new Promise((resolve) => setTimeout(resolve, ms));
	}

	private saveSettings(startSDM: boolean) {
		this.startingSDM = true;
		this.workspaceValid = window.SettingsApi?.isValidPath(
			this.settingsForm.controls['workspaceFolder'].value,
		);
		if (!this.workspaceValid) {
			this.startingSDM = false;
			return;
		}
		let settings: Settings = {
			id: this.settings?.id ?? -1,
			electionEventSeed: this.settingsForm.controls['electionEventSeed'].value,
			workspaceFolder: this.settingsForm.controls['workspaceFolder'].value,
			mode: this.settingsForm.controls['mode'].value,
			voterPortalHost: this.settingsForm.controls['voterPortalHost'].value,
			votingServerHost: this.settingsForm.controls['votingServerHost'].value,
			twoWaySSLLocation: this.settingsForm.controls['twoWaySSLLocation'].value,
			twoWaySSLPwdLocation: this.settingsForm.controls['twoWaySSLPwdLocation'].value,
			outputFolder: this.settingsForm.controls['outputFolder'].value,
			externalConfigurationFolder: this.settingsForm.controls['externalConfigurationFolder'].value,
			printFolder: this.settingsForm.controls['printFolder'].value,
			verifierFolder: this.settingsForm.controls['verifierFolder'].value,
			tallyFolder: this.settingsForm.controls['tallyFolder'].value,
			voterPortalConfigurationFolder: this.settingsForm.controls['voterPortalConfigurationFolder'].value,
			exportPwd: this.settingsForm.controls['password'].value,
			verifierDatasetPwd: this.settingsForm.controls['password'].value,
			choiceCodeGenerationChunkSize: this.settingsForm.controls['choiceCodeGenerationChunkSize'].value,
			directTrustLocation: this.settingsForm.controls['directTrustLocation'].value,
			directTrustPwdLocation: this.settingsForm.controls['directTrustPwdLocation'].value,
			proxyConfig: this.useProxy ? this.settingsForm.controls['proxyConfig'].value : '',
		};
		window.SettingsApi?.saveSettings(settings, startSDM);
	}
}
