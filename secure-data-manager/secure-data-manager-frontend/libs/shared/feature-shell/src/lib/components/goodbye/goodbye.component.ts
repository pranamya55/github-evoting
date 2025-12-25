/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Inject} from '@angular/core';

import {TranslateModule} from "@ngx-translate/core";
import {PageActionsComponent, PageTitleComponent} from "@sdm/shared-ui-components";
import {APP_NAME} from "@sdm/shared-util-types";

@Component({
	selector: 'sdm-goodbye',
	standalone: true,
	imports: [TranslateModule, PageActionsComponent, PageTitleComponent],
	templateUrl: './goodbye.component.html',
})
export class GoodbyeComponent {
	constructor(
		@Inject(APP_NAME) public readonly appName: string,
	) {
	}
}
