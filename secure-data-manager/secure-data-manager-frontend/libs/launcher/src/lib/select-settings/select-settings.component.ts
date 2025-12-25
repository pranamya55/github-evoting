/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */

import {Component, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';

import {Settings, WorkflowStatus} from '@sdm/shared-util-types';
import {PageTitleComponent} from '@sdm/shared-ui-components';

@Component({
	selector: 'sdm-select-settings',
	standalone: true,
	imports: [
		TranslateModule,
		FormsModule,
		PageTitleComponent
	],
	templateUrl: './select-settings.component.html',
})
export class SelectSettingsComponent implements OnInit {
	settingsList: Settings[] = [];
	selectedSettings: Settings | null = null;

	protected readonly WorkflowStatus = WorkflowStatus;

	constructor(private readonly router: Router) {
	}

	selectSettings() {
		this.router.navigate(['manage-settings'], {
			state: {settings: this.selectedSettings},
		});
	}

	copySettings() {
		this.router.navigate(['manage-settings'], {
			state: {
				settings: {
					...this.selectedSettings,
					id: -1,
					workspaceFolder: '',
					outputFolder: '',
					printFolder: '',
					verifierFolder: '',
					tallyFolder: '',
				},
			},
		});
	}

	addSettings() {
		this.router.navigate(['manage-settings'], {
			state: {
				settings: {
					id: -1,
				},
			},
		});
	}

	ngOnInit(): void {
		this.settingsList = window.SettingsApi?.getSettingsList();
	}
}
