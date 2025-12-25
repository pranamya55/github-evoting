/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {Component, Input, OnInit} from '@angular/core';

import {TranslateModule} from '@ngx-translate/core';
import {ExportInfo} from '@sdm/shared-util-types';
import {DataExchangeService} from '@sdm/shared-feature-data-exchange';

@Component({
	selector: 'sdm-export-information',
	standalone: true,
	imports: [TranslateModule],
	templateUrl: './export-information.component.html',
})
export class ExportInformationComponent implements OnInit {
	@Input({required: true}) exchangeIndex!: string;
	exportInformation?: ExportInfo;

	constructor(private readonly dataExchangeService: DataExchangeService) {
	}

	ngOnInit() {
		this.dataExchangeService
			.getExportInfo(this.exchangeIndex)
			.subscribe((exportInformation) => {
				this.exportInformation = exportInformation;
			});
	}
}
