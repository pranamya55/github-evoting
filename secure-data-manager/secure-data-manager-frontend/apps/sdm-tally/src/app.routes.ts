/*
 * (c) Copyright 2025 Swiss Post Ltd.
 */
import {DataCollectionComponent} from '@sdm/shared-feature-data-collection';
import {ImportComponent} from '@sdm/shared-feature-data-exchange';
import {SdmRoute, WorkflowStep} from '@sdm/shared-util-types';
import {AuthoriseDecryptComponent, DecryptComponent} from '@sdm/tally';

export const appRoutes: SdmRoute[] = [
	{
		path: 'tally-1',
		children: [
			{
				path: 'import-5',
				component: ImportComponent,
				title: 'dataExchange.import.title.5',
				data: {
					workflowStep: WorkflowStep.ImportFromOnline5,
					exchangeIndex: '5',
				},
			},
			{
				path: 'decrypt',
				data: {
					workflowStep: WorkflowStep.Decrypt,
				},
				children: [
					{
						path: '',
						component: DecryptComponent,
						title: 'decrypt.title',
					},
					{
						path: 'authorise',
						component: AuthoriseDecryptComponent,
						title: 'authoriseDecrypt.title',
					},
				],
			},
			{
				path: 'data-collect-tally',
				component: DataCollectionComponent,
				title: 'dataCollection.title.tally',
				data: {
					workflowStep: WorkflowStep.CollectDataVerifierTally,
					mode: 'tally',
				},
			},
			{path: '', redirectTo: 'data-collect-tally', pathMatch: 'full'},
		],
	},
];
